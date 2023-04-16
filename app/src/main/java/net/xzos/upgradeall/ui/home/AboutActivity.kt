package net.xzos.upgradeall.ui.home

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.content.ContextCompat
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.LayoutHomeTitleBarBinding
import net.xzos.upgradeall.databinding.LayoutHomeUpdatingCardBinding
import net.xzos.upgradeall.ui.base.BaseActivity
import net.xzos.upgradeall.utils.MiscellaneousUtils

class AboutActivity : BaseActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            @Composable
            fun AboutText() {
                Text(stringResource(R.string.about))
            }
            Scaffold(
                topBar = {
                    TopAppBar(title = { AboutText() },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    AndroidViewBinding(LayoutHomeTitleBarBinding::inflate)
                    AndroidViewBinding(LayoutHomeUpdatingCardBinding::inflate) {
                        ivIcon.setImageResource(R.drawable.ic_url)
                        tsTitle.setText(getString(R.string.official_website))
                        val url = "https://up-a.org/"
                        tvSubtitle.text = url
                        layoutCard.setOnClickListener {
                            MiscellaneousUtils.accessByBrowser(url, layoutCard.context)
                        }
                    }
                    AndroidViewBinding(LayoutHomeUpdatingCardBinding::inflate) {
                        tsTitle.setText(getString(R.string.donate))
                        val url = "https://afdian.net/a/inkflaw"
                        tvSubtitle.text = url
                        layoutCard.setOnClickListener {
                            MiscellaneousUtils.accessByBrowser(url, layoutCard.context)
                        }
                    }
                }
            }
        }
    }
}