use getter_appmanager::{AppManager, AppStatus, ExtendedAppManager};
use std::collections::HashMap;
use std::sync::Arc;
use tokio::runtime::Runtime;

// Global instances
lazy_static::lazy_static! {
    static ref RUNTIME: Runtime = Runtime::new().expect("Failed to create Tokio runtime");
    static ref BASE_MANAGER: Arc<AppManager> = Arc::new(AppManager::new());
    static ref EXTENDED_MANAGER: Arc<ExtendedAppManager> = Arc::new(ExtendedAppManager::new());
}

/// AppManager facade for Android integration
pub struct AppManagerFacade;

impl AppManagerFacade {
    /// Add a new app to the manager
    pub fn add_app(
        app_id: String,
        hub_uuid: String,
        app_data: HashMap<String, String>,
        hub_data: HashMap<String, String>,
    ) -> Result<String, String> {
        RUNTIME.block_on(async {
            BASE_MANAGER.add_app(app_id, hub_uuid, app_data, hub_data).await
        })
    }

    /// Remove an app from the manager
    pub fn remove_app(app_id: &str) -> Result<bool, String> {
        RUNTIME.block_on(async {
            BASE_MANAGER.remove_app(app_id).await
        })
    }

    /// List all apps
    pub fn list_apps() -> Result<Vec<String>, String> {
        RUNTIME.block_on(async {
            BASE_MANAGER.list_apps().await
        })
    }

    /// Get app status (as alternative to get_app)
    pub fn get_app(app_id: &str) -> Result<Option<HashMap<String, String>>, String> {
        RUNTIME.block_on(async {
            // Get app status and convert to HashMap
            match BASE_MANAGER.get_app_status(app_id).await {
                Ok(Some(status)) => {
                    let mut map = HashMap::new();
                    map.insert("app_id".to_string(), status.app_id);
                    map.insert("status".to_string(), format!("{:?}", status.status));
                    if let Some(cv) = status.current_version {
                        map.insert("current_version".to_string(), cv);
                    }
                    if let Some(lv) = status.latest_version {
                        map.insert("latest_version".to_string(), lv);
                    }
                    Ok(Some(map))
                }
                Ok(None) => Ok(None),
                Err(e) => Err(e),
            }
        })
    }

    /// Update app to specific version
    pub fn update_app(
        app_id: &str,
        version: String,
    ) -> Result<bool, String> {
        RUNTIME.block_on(async {
            BASE_MANAGER.update_app(app_id, &version).await.map(|_| true)
        })
    }

    /// Get app status
    pub fn get_app_status(app_id: &str) -> Result<Option<getter_appmanager::AppStatusInfo>, String> {
        RUNTIME.block_on(async {
            BASE_MANAGER.get_app_status(app_id).await
        })
    }

    /// Get all app statuses
    pub fn get_all_app_statuses() -> Result<Vec<getter_appmanager::AppStatusInfo>, String> {
        RUNTIME.block_on(async {
            BASE_MANAGER.get_all_app_statuses().await
        })
    }

    /// Get outdated apps
    pub fn get_outdated_apps() -> Result<Vec<getter_appmanager::AppStatusInfo>, String> {
        RUNTIME.block_on(async {
            BASE_MANAGER.get_outdated_apps().await
        })
    }

    /// Refresh app status (triggers status update)
    pub fn refresh_app_status(app_id: &str) -> Result<(), String> {
        RUNTIME.block_on(async {
            // Trigger a status update by getting the current status
            BASE_MANAGER.get_app_status(app_id).await.map(|_| ())
        })
    }

    /// Refresh all app statuses
    pub fn refresh_all_statuses() -> Result<(), String> {
        RUNTIME.block_on(async {
            // Trigger status updates by getting all statuses
            BASE_MANAGER.get_all_app_statuses().await.map(|_| ())
        })
    }

    // ========== Extended Manager Functions ==========

    /// Set star status for an app
    pub fn set_app_star(app_id: &str, star: bool) -> Result<bool, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.set_app_star(app_id, star).await
        })
    }

    /// Check if an app is starred
    pub fn is_app_starred(app_id: &str) -> bool {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.is_app_starred(app_id).await
        })
    }

    /// Get all starred app IDs
    pub fn get_starred_apps() -> Result<Vec<String>, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.get_starred_apps().await
        })
    }

    /// Set ignored version for an app
    pub fn set_ignore_version(app_id: &str, version: &str) -> Result<bool, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.set_ignore_version(app_id, version).await
        })
    }

    /// Get ignored version for an app
    pub fn get_ignore_version(app_id: &str) -> Result<Option<String>, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.get_ignore_version(app_id).await
        })
    }

    /// Check if a version is ignored
    pub fn is_version_ignored(app_id: &str, version: &str) -> bool {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.is_version_ignored(app_id, version).await
        })
    }

    /// Ignore all current versions
    pub fn ignore_all_current_versions() -> Result<u32, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.ignore_all_current_versions().await
        })
    }

    /// Get apps by type
    pub fn get_apps_by_type(app_type: &str) -> Result<Vec<String>, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.get_apps_by_type(app_type).await
        })
    }

    /// Get apps by status
    pub fn get_apps_by_status(status: AppStatus) -> Result<Vec<getter_appmanager::AppStatusInfo>, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.get_apps_by_status(status).await
        })
    }

    /// Get starred apps with their status
    pub fn get_starred_apps_with_status() -> Result<Vec<getter_appmanager::AppStatusInfo>, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.get_starred_apps_with_status().await
        })
    }

    /// Get outdated apps excluding ignored versions
    pub fn get_outdated_apps_filtered() -> Result<Vec<getter_appmanager::AppStatusInfo>, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.get_outdated_apps_filtered().await
        })
    }

    /// Add app with observer notification
    pub fn add_app_with_notification(
        app_id: String,
        hub_uuid: String,
        app_data: HashMap<String, String>,
        hub_data: HashMap<String, String>,
    ) -> Result<String, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.add_app_with_notification(app_id, hub_uuid, app_data, hub_data).await
        })
    }

    /// Remove app with observer notification
    pub fn remove_app_with_notification(app_id: &str) -> Result<bool, String> {
        RUNTIME.block_on(async {
            EXTENDED_MANAGER.remove_app_with_notification(app_id).await
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_star_management() {
        let app_id = "test.app.star";
        
        // Set star
        let result = AppManagerFacade::set_app_star(app_id, true);
        assert!(result.is_ok());
        
        // Check star
        let is_starred = AppManagerFacade::is_app_starred(app_id);
        assert!(is_starred);
        
        // Unset star
        let result = AppManagerFacade::set_app_star(app_id, false);
        assert!(result.is_ok());
        
        // Check star again
        let is_starred = AppManagerFacade::is_app_starred(app_id);
        assert!(!is_starred);
    }

    #[test]
    fn test_version_ignore() {
        let app_id = "test.app.version";
        let version = "1.0.0";
        
        // Set ignore version
        let result = AppManagerFacade::set_ignore_version(app_id, version);
        assert!(result.is_ok());
        
        // Check if ignored
        let is_ignored = AppManagerFacade::is_version_ignored(app_id, version);
        assert!(is_ignored);
        
        // Check different version
        let is_ignored = AppManagerFacade::is_version_ignored(app_id, "2.0.0");
        assert!(!is_ignored);
    }

    #[test]
    fn test_list_apps() {
        let result = AppManagerFacade::list_apps();
        assert!(result.is_ok());
    }

    #[test]
    fn test_get_starred_apps() {
        let result = AppManagerFacade::get_starred_apps();
        assert!(result.is_ok());
    }
}