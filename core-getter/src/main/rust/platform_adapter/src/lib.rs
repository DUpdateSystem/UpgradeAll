//! Rust-active platform capability adapter for the UpgradeAll Android product.
//!
//! This crate intentionally lives outside the reusable getter submodule. It
//! defines platform facts and Android runtime plumbing for the product/native
//! bridge layer. getter still owns domain decisions such as package ids,
//! repository coverage, Lua generation, and storage writes.

use serde::{Deserialize, Serialize};

#[cfg(target_os = "android")]
pub mod android;

pub const INSTALLED_INVENTORY_FORMAT: &str = "upgradeall-installed-inventory";
pub const INSTALLED_INVENTORY_VERSION: u32 = 1;

/// A small Rust-owned interface for platform capabilities.
///
/// Implementations return platform facts only. Callers must not infer getter
/// product decisions from this interface; the native bridge/getter operation is
/// responsible for converting facts into getter-owned workflows.
pub trait PlatformAdapter: Send + Sync {
    fn scan_installed_inventory(
        &self,
        options: InstalledInventoryScanOptions,
    ) -> Result<InstalledInventoryScanResult, PlatformAdapterError>;
}

/// Host/test adapter used when no platform implementation is available.
#[derive(Debug, Default)]
pub struct NoopPlatformAdapter;

impl PlatformAdapter for NoopPlatformAdapter {
    fn scan_installed_inventory(
        &self,
        _options: InstalledInventoryScanOptions,
    ) -> Result<InstalledInventoryScanResult, PlatformAdapterError> {
        Err(PlatformAdapterError::Unsupported {
            capability: "installed_inventory",
        })
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct InstalledInventoryScanOptions {
    #[serde(default)]
    pub include_system_apps: bool,
    #[serde(default)]
    pub include_self: bool,
}

impl Default for InstalledInventoryScanOptions {
    fn default() -> Self {
        Self {
            include_system_apps: false,
            include_self: false,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct InstalledInventoryScanResult {
    pub inventory: InstalledInventory,
    pub stats: InstalledInventoryScanStats,
    #[serde(default)]
    pub diagnostics: Vec<PlatformDiagnostic>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct InstalledInventory {
    pub format: String,
    pub version: u32,
    #[serde(default)]
    pub items: Vec<InstalledInventoryItem>,
}

impl InstalledInventory {
    pub fn new(items: Vec<InstalledInventoryItem>) -> Self {
        Self {
            format: INSTALLED_INVENTORY_FORMAT.to_owned(),
            version: INSTALLED_INVENTORY_VERSION,
            items,
        }
    }
}

/// Getter-compatible installed inventory facts produced by platform code.
///
/// Android platform adapters emit raw package names and metadata only. They do
/// not normalize to `android/<package>` package ids. Magisk facts are excluded
/// from this PackageManager adapter surface and need a separate capability
/// decision.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum InstalledInventoryItem {
    AndroidPackage {
        package_name: String,
        #[serde(default)]
        label: Option<String>,
        #[serde(default)]
        version_name: Option<String>,
        #[serde(default)]
        version_code: Option<i64>,
    },
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct InstalledInventoryScanStats {
    pub total_seen: u32,
    pub returned: u32,
    pub filtered_system: u32,
    pub filtered_self: u32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct PlatformDiagnostic {
    pub code: String,
    pub message: String,
    #[serde(default)]
    pub detail: Option<String>,
}

#[derive(Debug, thiserror::Error)]
pub enum PlatformAdapterError {
    #[error("platform capability '{capability}' is unsupported")]
    Unsupported { capability: &'static str },
    #[error("platform adapter is not initialized")]
    NotInitialized,
    #[error("platform adapter JNI error: {0}")]
    Jni(String),
    #[error("platform adapter response is malformed: {0}")]
    MalformedResponse(String),
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn scan_options_default_to_privacy_preserving_user_inventory() {
        let options = InstalledInventoryScanOptions::default();

        assert!(!options.include_system_apps);
        assert!(!options.include_self);

        let json = serde_json::to_value(options).expect("serialize options");
        assert_eq!(json["include_system_apps"], false);
        assert_eq!(json["include_self"], false);
    }

    #[test]
    fn inventory_serializes_to_getter_compatible_android_package_facts() {
        let inventory = InstalledInventory::new(vec![InstalledInventoryItem::AndroidPackage {
            package_name: "org.fdroid.fdroid".to_owned(),
            label: Some("F-Droid".to_owned()),
            version_name: Some("1.20.0".to_owned()),
            version_code: Some(1_020_000),
        }]);

        let json = serde_json::to_value(&inventory).expect("serialize inventory");

        assert_eq!(json["format"], INSTALLED_INVENTORY_FORMAT);
        assert_eq!(json["version"], INSTALLED_INVENTORY_VERSION);
        assert_eq!(json["items"][0]["kind"], "android_package");
        assert_eq!(json["items"][0]["package_name"], "org.fdroid.fdroid");
        assert!(json["items"][0].get("package_id").is_none());
    }

    #[test]
    fn scan_result_deserializes_with_default_diagnostics() {
        let json = r#"
        {
          "inventory": {
            "format": "upgradeall-installed-inventory",
            "version": 1,
            "items": []
          },
          "stats": {
            "total_seen": 3,
            "returned": 1,
            "filtered_system": 1,
            "filtered_self": 1
          }
        }
        "#;

        let result: InstalledInventoryScanResult =
            serde_json::from_str(json).expect("deserialize scan result");

        assert!(result.diagnostics.is_empty());
        assert_eq!(result.stats.total_seen, 3);
        assert_eq!(result.inventory.items, Vec::new());
    }

    #[test]
    fn noop_adapter_reports_unsupported_installed_inventory() {
        let adapter = NoopPlatformAdapter;

        let error = adapter
            .scan_installed_inventory(InstalledInventoryScanOptions::default())
            .expect_err("noop adapter should not scan");

        assert!(matches!(
            error,
            PlatformAdapterError::Unsupported {
                capability: "installed_inventory"
            }
        ));
    }

    #[test]
    fn platform_inventory_json_is_accepted_by_getter_core_autogen_schema() {
        let inventory = InstalledInventory::new(vec![InstalledInventoryItem::AndroidPackage {
            package_name: "org.fdroid.fdroid".to_owned(),
            label: Some("F-Droid".to_owned()),
            version_name: Some("1.20.0".to_owned()),
            version_code: Some(1_020_000),
        }]);
        let json = serde_json::to_string(&inventory).expect("serialize platform inventory");

        let getter_inventory: getter_core::autogen::InstalledInventory =
            serde_json::from_str(&json).expect("getter-core should accept platform inventory");

        getter_core::autogen::validate_installed_inventory(&getter_inventory)
            .expect("inventory format/version should match getter core");
        assert_eq!(getter_inventory.items.len(), 1);
    }
}
