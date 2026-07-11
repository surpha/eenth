package com.eenth.blocker

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isBlocked: Boolean
)

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onToggle: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivAppIcon)
        val name: TextView = view.findViewById(R.id.tvAppName)
        val packageLabel: TextView = view.findViewById(R.id.tvPackageName)
        val switch: SwitchMaterial = view.findViewById(R.id.switchBlocked)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.packageLabel.text = app.packageName

        // Prevent triggering listener during rebind
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = app.isBlocked
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            app.isBlocked = isChecked
            onToggle(app, isChecked)
        }

        // Allow tapping the whole row to toggle
        holder.itemView.setOnClickListener {
            holder.switch.isChecked = !holder.switch.isChecked
        }
    }

    override fun getItemCount(): Int = apps.size
}
