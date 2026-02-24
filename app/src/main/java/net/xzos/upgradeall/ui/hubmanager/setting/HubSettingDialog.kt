package net.xzos.upgradeall.ui.hubmanager.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntityManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.utils.URLReplaceData
import net.xzos.upgradeall.databinding.BottomsheetHubSettingBinding

class HubSettingDialog : BottomSheetDialogFragment() {

    private var _binding: BottomsheetHubSettingBinding? = null
    private val binding get() = _binding!!

    private var hubUuid: String? = null
    private lateinit var authAdapter: AuthEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hubUuid = arguments?.getString(ARG_HUB_UUID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomsheetHubSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAuthSection()
        setupUrlReplaceSection()
        setupButtons()
    }

    private fun setupAuthSection() {
        val uuid = hubUuid
        if (uuid == null) {
            // Global settings: hide authentication section
            binding.authSection.visibility = View.GONE
            binding.authDivider.visibility = View.GONE
            return
        }

        val hub = HubManager.getHub(uuid)
        val keyHints = hub?.hubConfig?.authKeywords ?: emptyList()
        val currentAuth = hub?.auth?.entries?.map { it.key to it.value }?.toMutableList()
            ?: mutableListOf()

        authAdapter = AuthEntryAdapter(currentAuth, keyHints)
        binding.authRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = authAdapter
        }

        binding.authAddButton.setOnClickListener {
            authAdapter.addEntry()
        }
    }

    private fun setupUrlReplaceSection() {
        lifecycleScope.launch(Dispatchers.IO) {
            val extraHubEntity = ExtraHubEntityManager.getExtraHub(hubUuid)
            withContext(Dispatchers.Main) {
                val useGlobal = extraHubEntity?.global ?: false
                binding.enableUrlReplaceSwitch.isChecked = useGlobal
                binding.matchRuleEdit.setText(extraHubEntity?.urlReplaceSearch ?: "")
                binding.replaceStringEdit.setText(extraHubEntity?.urlReplaceString ?: "")

                // Only show the "use global" switch for per-hub dialogs
                binding.enableUrlReplaceSwitch.visibility =
                    if (hubUuid != null) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupButtons() {
        binding.cancelButton.setOnClickListener { dismiss() }

        binding.saveButton.setOnClickListener {
            val uuid = hubUuid
            val useGlobal = binding.enableUrlReplaceSwitch.isChecked
            val matchRule = binding.matchRuleEdit.text?.toString()
            val replaceString = binding.replaceStringEdit.text?.toString()

            lifecycleScope.launch(Dispatchers.IO) {
                // Save URL replace rules
                ExtraHubEntityManager.setUrlReplace(
                    uuid,
                    useGlobal,
                    URLReplaceData(matchRule, replaceString),
                )

                // Save auth credentials (only for per-hub dialogs)
                if (uuid != null) {
                    val authMap = authAdapter.toAuthMap()
                    HubManager.updateHubAuth(uuid, authMap)
                }

                withContext(Dispatchers.Main) { dismiss() }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_HUB_UUID = "hub_uuid"
        const val TAG = "HubSettingDialog"

        fun newInstance(hubUuid: String? = null): HubSettingDialog =
            HubSettingDialog().apply {
                arguments = Bundle().apply {
                    if (hubUuid != null) putString(ARG_HUB_UUID, hubUuid)
                }
            }
    }
}
