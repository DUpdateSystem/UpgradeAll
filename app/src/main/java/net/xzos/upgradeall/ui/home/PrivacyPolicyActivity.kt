package net.xzos.upgradeall.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import net.xzos.upgradeall.data.PreferencesMap

private const val PRIVACY_POLICY_TEXT = """
# UpgradeAll 隐私政策

最后更新：2023 年 4 月 19 日

感谢您选择使用 UpgradeAll（以下简称为“本应用”）。我们非常重视您的隐私，并致力于保护您的个人信息。在使用本应用之前，请仔细阅读本隐私政策，以了解我们如何收集、使用、保护以及披露您的个人信息。

## 1. 信息收集

本应用基于本地爬虫，完全在您的设备上运行。我们不会收集、储存或传输您的任何个人信息。

### 1.1 本地存储

本应用将以下信息存储在您的设备上：

- 您在本应用中配置的设置信息；
- 您安装的 Android 应用、Magisk 模块等的列表和更新状态。

这些信息仅在您的设备上存储，不会传输至我们的服务器。

## 2. 信息使用

我们仅使用存储在您设备上的信息来提供本应用的功能，包括但不限于检查更新、显示更新列表等。我们不会将这些信息用于任何其他目的。

## 3. 信息披露

本应用承诺不会向任何第三方披露、出售、转让或租赁您的个人信息。

## 4. 信息安全

本应用采用严格的安全措施，以保护您的个人信息免受未经授权的访问、使用或披露。然而，尽管我们已经采取了合理的安全措施，但请您注意，没有任何一种安全措施是完全可靠的。

## 5. 隐私政策的更改

我们可能会不时更新本隐私政策。如有更改，我们将通过本应用向您发布新的隐私政策，并更新“最后更新”日期。在您继续使用本应用之前，请务必查看最新的隐私政策。

## 6. 联系我们

如果您对本隐私政策有任何疑问或意见，请通过以下方式与我们联系：

- 电子邮件：[support@example.com](mailto:support@example.com)

感谢您阅读本隐私政策，祝您使用本应用愉快！

"""

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())

            ) {
                Text(PRIVACY_POLICY_TEXT)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(
                        onClick = {
                            PreferencesMap.checked_privacy_policy = true
                            finish()
                        }
                    ) {
                        Text(text = "Continue")
                    }
                }
            }
        }
    }
}