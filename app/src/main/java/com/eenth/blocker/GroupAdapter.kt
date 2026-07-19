package com.eenth.blocker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupAdapter(
    private val groups: MutableList<AppGroup>,
    private val onClick: (AppGroup) -> Unit,
    private val onLongPress: (AppGroup) -> Unit
) : RecyclerView.Adapter<GroupAdapter.ViewHolder>() {

    var isBricked: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: LinearLayout = view.findViewById(R.id.groupCard)
        val iconContainer: FrameLayout = view.findViewById(R.id.iconContainer)
        val name: TextView = view.findViewById(R.id.tvGroupName)
        val count: TextView = view.findViewById(R.id.tvGroupCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groups[position]
        val context = holder.itemView.context
        val pm = context.packageManager
        val density = context.resources.displayMetrics.density

        // Grid spacing
        val gap = (6 * density).toInt()
        val lp = holder.itemView.layoutParams as? RecyclerView.LayoutParams
            ?: RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = gap * 2
        lp.marginStart = if (position % 2 == 0) 0 else gap
        lp.marginEnd = if (position % 2 == 0) gap else 0
        holder.itemView.layoutParams = lp

        holder.name.text = group.name
        holder.count.text = "${group.packages.size} apps"

        // Build overlapping app icons
        holder.iconContainer.removeAllViews()
        val iconSize = (30 * density).toInt()
        val step = (20 * density).toInt()
        val maxIcons = 4

        group.packages.toList().take(maxIcons).forEachIndexed { index, pkg ->
            val iv = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    marginStart = index * step
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                try { setImageDrawable(pm.getApplicationIcon(pkg)) }
                catch (_: Exception) { setImageResource(android.R.drawable.sym_def_app_icon) }
                elevation = (maxIcons - index).toFloat() * density
            }
            holder.iconContainer.addView(iv)
        }

        if (group.packages.size > maxIcons) {
            val badge = TextView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = maxIcons * step
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                text = "+${group.packages.size - maxIcons}"
                setTextColor(0xFF8E8E93.toInt())
                textSize = 10f
            }
            holder.iconContainer.addView(badge)
        }

        // Selection state — green when unblocked, red when blocked
        if (group.isSelected) {
            if (isBricked) {
                holder.card.setBackgroundResource(R.drawable.bg_card_selected_red)
            } else {
                holder.card.setBackgroundResource(R.drawable.bg_card_selected)
            }
        } else {
            holder.card.setBackgroundResource(R.drawable.bg_card)
        }

        holder.card.setOnClickListener {
            onClick(group)
        }

        holder.card.setOnLongClickListener {
            onLongPress(group)
            true
        }
    }

    override fun getItemCount(): Int = groups.size

    fun updateGroups(newGroups: List<AppGroup>) {
        groups.clear()
        groups.addAll(newGroups)
        notifyDataSetChanged()
    }
}
