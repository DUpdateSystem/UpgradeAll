package net.xzos.upgradeall.ui.hubmanager.setting

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.databinding.ItemAuthEntryBinding

/**
 * RecyclerView adapter for editing hub authentication key-value pairs.
 *
 * @param entries   Mutable list of (key, value) pairs to edit in-place.
 * @param keyHints  Autocomplete suggestions for the key field (from hub.hubConfig.authKeywords).
 */
class AuthEntryAdapter(
    private val entries: MutableList<Pair<String, String>>,
    private val keyHints: List<String>,
) : RecyclerView.Adapter<AuthEntryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAuthEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val (key, value) = entries[position]

            // Populate autocomplete suggestions for the key field
            val context = binding.root.context
            val hintAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, keyHints)
            binding.authKeyEdit.setAdapter(hintAdapter)
            binding.authKeyEdit.setText(key)

            // Set current value (avoid triggering password toggle animation)
            binding.authValueEdit.setText(value)

            // Save edits back to the list on text change
            binding.authKeyEdit.setOnFocusChangeListener { _, _ ->
                entries[position] = binding.authKeyEdit.text.toString() to
                        (binding.authValueEdit.text?.toString() ?: "")
            }
            binding.authValueEdit.setOnFocusChangeListener { _, _ ->
                entries[position] = (binding.authKeyEdit.text?.toString() ?: "") to
                        (binding.authValueEdit.text?.toString() ?: "")
            }

            binding.authDeleteButton.setOnClickListener {
                entries.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, entries.size)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAuthEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = entries.size

    /** Flush currently focused text fields into the backing list before reading. */
    fun flushEntries() {
        // Focus changes trigger saves, but call this before collecting data to be safe.
        notifyDataSetChanged()
    }

    /** Return a snapshot of current non-empty entries as a Map. */
    fun toAuthMap(): Map<String, String> =
        entries.filter { (k, _) -> k.isNotBlank() }.toMap()

    /** Append a blank entry row. */
    fun addEntry() {
        entries.add("" to "")
        notifyItemInserted(entries.size - 1)
    }
}
