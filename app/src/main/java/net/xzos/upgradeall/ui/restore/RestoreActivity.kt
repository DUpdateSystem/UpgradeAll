package net.xzos.upgradeall.ui.restore

import android.annotation.SuppressLint
import android.view.View
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.app.backup.manager.RestoreManager
import net.xzos.upgradeall.app.backup.manager.status.RestoreStage
import net.xzos.upgradeall.app.backup.manager.status.RestoreStatus
import net.xzos.upgradeall.core.androidutils.runUiFun
import net.xzos.upgradeall.databinding.ActivityRestoreBinding
import net.xzos.upgradeall.ui.base.AppBarActivity

class RestoreActivity : AppBarActivity() {
    private lateinit var binding: ActivityRestoreBinding
    override fun initBinding(): View {
        binding = ActivityRestoreBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {
        val observerFun = fun(status: RestoreStatus) {
            runUiFun {
                if (status.stage == RestoreStage.FINISH)
                    this.finish()
                binding.textViewStage.text = status.stage.msg
                val process = status.progress.currentIndex / status.progress.total * 100
                binding.progressBar.progress = process
                @SuppressLint("SetTextI18n")
                binding.textViewProgress.text =
                    "${status.progress.currentIndex}/${status.progress.total}"
                binding.textViewNote.text = status.stepNote
            }
        }
        RestoreManager.observeForever(observerFun)
    }
}