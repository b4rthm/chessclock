package com.barth.chessclock

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.barth.chessclock.databinding.ActivityMainBinding
import com.barth.chessclock.model.ChessClock
import com.barth.chessclock.model.TimeControl
import com.barth.chessclock.model.TimeControlCategory
import com.barth.chessclock.model.TimeControlPreset
import com.barth.chessclock.model.TimeControlPresets
import com.barth.chessclock.model.formatClockTime
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.roundToInt

/**
 * Single-activity UI: a setup screen (time control) and a game screen where
 * the two clocks are the buttons — press the clock of the side to move to
 * complete its move, just like a real chess clock.
 *
 * The clock model ([ChessClock]) is driven by a main-thread ticker that
 * measures the *real* elapsed time between frames, so the display neither
 * drifts nor loses time across pauses. No background threads involved.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: SettingsStore
    private lateinit var settings: ClockSettings

    private var clock: ChessClock? = null
    private var inGame = false
    private var currentCategory = SettingsStore.DEFAULT.category
    private var pauseShowsPlayIcon = false

    /** Guards programmatic chip/input updates against re-triggering listeners. */
    private var applyingPreset = false

    private val rotatedTimeSizePx by lazy { spToPx(ROTATED_TIME_SP) }

    private val panelBgNormal by lazy { ContextCompat.getDrawable(this, R.drawable.clock_panel)!! }
    private val panelBgActive by lazy { ContextCompat.getDrawable(this, R.drawable.clock_panel_active)!! }
    private val panelBgLight by lazy { ContextCompat.getDrawable(this, R.drawable.clock_panel_white)!! }
    private val panelBgLightActive by lazy { ContextCompat.getDrawable(this, R.drawable.clock_panel_white_active)!! }
    private val panelBgDark by lazy { ContextCompat.getDrawable(this, R.drawable.clock_panel_black)!! }
    private val panelBgDarkActive by lazy { ContextCompat.getDrawable(this, R.drawable.clock_panel_black_active)!! }

    private val handler = Handler(Looper.getMainLooper())
    private var lastTickAt = SystemClock.elapsedRealtime()

    private val ticker = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - lastTickAt
            lastTickAt = now
            clock?.tick(elapsed) // no-op while paused, finished, or in setup
            if (inGame) renderClocks()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        store = SettingsStore(this)
        settings = store.load()

        // 1) Apply persisted state first (no listeners attached yet).
        applyInputsFrom(settings)
        selectCategory(settings.category)
        applyLayoutRotation()

        // 2) Attach listeners only after state is applied.
        binding.categoryChips.setOnCheckedStateChangeListener { group, _ ->
            if (applyingPreset) return@setOnCheckedStateChangeListener
            val id = group.checkedChipId
            if (id == View.NO_ID) return@setOnCheckedStateChangeListener
            val category = categoryOf(group.findViewById(id))
                ?: return@setOnCheckedStateChangeListener
            selectCategory(category)
        }
        rebuildPresetChips()
        val onManualEdit = { if (!applyingPreset) deselectPresetIfMismatch() }
        binding.baseInput.doAfterTextChanged { onManualEdit() }
        binding.incrementInput.doAfterTextChanged { onManualEdit() }

        binding.startButton.setOnClickListener { onStartClicked() }
        binding.whitePanel.setOnClickListener { onPanelTap(ChessClock.Player.WHITE) }
        binding.blackPanel.setOnClickListener { onPanelTap(ChessClock.Player.BLACK) }
        binding.pause.setOnClickListener {
            clock?.togglePause()
            renderClocks()
        }
        binding.restart.setOnClickListener { confirmRestart() }
        binding.settingsButton.setOnClickListener { showSettingsDialog() }
        // Tapping empty space dismisses the keyboard (setup screen only).
        binding.root.setOnTouchListener { _, _ ->
            if (binding.setup.visibility == View.VISIBLE) hideKeyboard()
            false
        }
    }

    override fun onResume() {
        super.onResume()
        // Restart the ticker without counting the time we were away.
        lastTickAt = SystemClock.elapsedRealtime()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    // ------------------------------- setup screen -------------------------------

    private fun selectCategory(category: TimeControlCategory) {
        currentCategory = category
        applyingPreset = true
        when (category) {
            TimeControlCategory.BULLET -> binding.bulletChip.isChecked = true
            TimeControlCategory.BLITZ -> binding.blitzChip.isChecked = true
            TimeControlCategory.RAPID -> binding.rapidChip.isChecked = true
            TimeControlCategory.CLASSICAL -> binding.classicalChip.isChecked = true
        }
        rebuildPresetChips()
        applyingPreset = false
    }

    private fun categoryOf(chip: View?): TimeControlCategory? = when (chip?.id) {
        R.id.bulletChip -> TimeControlCategory.BULLET
        R.id.blitzChip -> TimeControlCategory.BLITZ
        R.id.rapidChip -> TimeControlCategory.RAPID
        R.id.classicalChip -> TimeControlCategory.CLASSICAL
        else -> null
    }

    private fun rebuildPresetChips() {
        binding.presetChips.removeAllViews()
        val selectedId = matchingPresetId()
        applyingPreset = true
        for (preset in TimeControlPresets.forCategory(currentCategory)) {
            val chip = Chip(this).apply {
                text = preset.control.label
                tag = preset
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked && !applyingPreset) applyPresetValues(preset)
                }
                if (preset.id == selectedId) isChecked = true
            }
            binding.presetChips.addView(chip)
        }
        applyingPreset = false
    }

    /** A preset matching the current input values, or null (custom control). */
    private fun matchingPresetId(): String? {
        val base = parseMinutes(binding.baseInput.text) ?: return null
        val baseSeconds = (base * 60).roundToInt()
        val increment = parseSeconds(binding.incrementInput.text) ?: return null
        return TimeControlPresets.forCategory(currentCategory).firstOrNull {
            it.control.baseSeconds == baseSeconds && it.control.incrementSeconds == increment
        }?.id
    }

    private fun inputsMatch(preset: TimeControlPreset): Boolean {
        val base = parseMinutes(binding.baseInput.text) ?: return false
        val increment = parseSeconds(binding.incrementInput.text) ?: return false
        return base * 60 == preset.control.baseSeconds.toDouble() &&
            increment == preset.control.incrementSeconds
    }

    private fun deselectPresetIfMismatch() {
        val id = binding.presetChips.checkedChipId
        if (id == View.NO_ID) return
        val chip = binding.presetChips.findViewById<Chip>(id) ?: return
        val preset = chip.tag as? TimeControlPreset ?: return
        if (!inputsMatch(preset)) {
            applyingPreset = true
            chip.isChecked = false
            applyingPreset = false
        }
    }

    private fun applyInputsFrom(settings: ClockSettings) {
        binding.baseInput.setText(formatMinutesForInput(settings.baseMinutes))
        binding.incrementInput.setText(settings.incrementSeconds.toString())
    }

    private fun applyPresetValues(preset: TimeControlPreset) {
        applyingPreset = true
        binding.baseInput.setText(formatMinutesForInput(preset.control.baseSeconds / 60.0))
        binding.incrementInput.setText(preset.control.incrementSeconds.toString())
        applyingPreset = false
    }

    private fun onStartClicked() {
        hideKeyboard()
        val baseMinutes = parseMinutes(binding.baseInput.text)
        val increment = parseSeconds(binding.incrementInput.text)
        if (baseMinutes == null || baseMinutes !in BASE_MIN_RANGE) {
            toast(R.string.invalid_base)
            return
        }
        if (increment == null || increment > MAX_BONUS_SECONDS) {
            toast(R.string.invalid_increment)
            return
        }
        settings = store.save(settings, baseMinutes, increment, currentCategory)
        startGame(TimeControl(baseSeconds = (baseMinutes * 60).roundToInt(), increment))
    }

    // ------------------------------- game screen -------------------------------

    private fun startGame(control: TimeControl) {
        clock = ChessClock(control).also { it.start() }
        inGame = true
        binding.setup.visibility = View.GONE
        binding.gameRoot.visibility = View.VISIBLE
        applyLayoutRotation()
        renderClocks()
    }

    private fun restart() {
        clock = null
        inGame = false
        binding.gameRoot.visibility = View.GONE
        binding.setup.visibility = View.VISIBLE
    }

    /** Restarting loses the running game, so ask first. */
    private fun confirmRestart() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.restart)
            .setMessage(R.string.restart_confirm)
            .setPositiveButton(R.string.confirm) { _, _ -> restart() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Small in-game settings: the layout toggle and the design toggle.
     * Each row shows its current value; tapping a row applies + persists
     * immediately, so the dialog is just a place to reach both controls.
     */
    private fun showSettingsDialog() {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val attr = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, attr, true)

        fun makeRow(labelRes: Int): Pair<LinearLayout, TextView> {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(20), dp(14), dp(20), dp(14))
                if (attr.resourceId != 0) {
                    background = ContextCompat.getDrawable(this@MainActivity, attr.resourceId)
                }
                isClickable = true
            }
            val label = TextView(this).apply {
                text = getString(labelRes)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                textSize = 16f
            }
            val value = TextView(this).apply {
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent))
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
            }
            row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(value)
            return row to value
        }

        val (layoutRow, layoutValue) = makeRow(R.string.row_layout)
        val (designRow, designValue) = makeRow(R.string.row_design)
        val refresh = {
            layoutValue.text = getString(
                if (settings.layoutRotation % 2 == 1) R.string.layout_black_top
                else R.string.layout_white_top,
            )
            designValue.text = getString(
                if (settings.design == 1) R.string.design_contrast else R.string.design_default,
            )
        }
        layoutRow.setOnClickListener {
            settings = store.saveLayoutRotation(settings, settings.layoutRotation + 1)
            applyLayoutRotation()
            refresh()
        }
        designRow.setOnClickListener {
            settings = store.saveDesign(settings, 1 - settings.design)
            renderClocks()
            refresh()
        }
        refresh()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(layoutRow)
            addView(designRow)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setView(content)
            .setPositiveButton(R.string.done, null)
            .show()
    }

    /** The clocks are the buttons: only the side to move can be pressed. */
    private fun onPanelTap(player: ChessClock.Player) {
        val c = clock ?: return
        if (!c.isRunning || c.isGameOver || c.toMove != player) return
        c.changeTurn()
        renderClocks()
    }

    private fun renderClocks() {
        val c = clock ?: return
        val gameOver = c.isGameOver
        val contrast = settings.design == 1
        // In the contrast design the boxes are literally white/black, so the
        // text uses the opposite color to stay readable.
        renderSide(
            binding.whitePanel, binding.whiteTime, binding.whiteStatus,
            c.whiteTimeMs, c.toMove == ChessClock.Player.WHITE,
            gameOver, c.winner == ChessClock.Player.WHITE,
            baseColorRes = if (contrast) R.color.text_on_light else R.color.text_primary,
            dimColorRes = if (contrast) R.color.text_on_light_dim else R.color.text_dim,
            normalBg = if (contrast) panelBgLight else panelBgNormal,
            activeBg = if (contrast) panelBgLightActive else panelBgActive,
        )
        renderSide(
            binding.blackPanel, binding.blackTime, binding.blackStatus,
            c.blackTimeMs, c.toMove == ChessClock.Player.BLACK,
            gameOver, c.winner == ChessClock.Player.BLACK,
            baseColorRes = R.color.text_primary,
            dimColorRes = R.color.text_dim,
            normalBg = if (contrast) panelBgDark else panelBgNormal,
            activeBg = if (contrast) panelBgDarkActive else panelBgActive,
        )
        binding.pause.isEnabled = !gameOver
        val resuming = !c.isRunning
        binding.pause.contentDescription =
            getString(if (c.isRunning) R.string.pause else R.string.resume)
        if (pauseShowsPlayIcon != resuming) {
            pauseShowsPlayIcon = resuming
            binding.pause.setImageResource(
                if (resuming) R.drawable.ic_play else R.drawable.ic_pause,
            )
        }
    }

    private fun renderSide(
        panel: FrameLayout,
        timeView: TextView,
        statusView: TextView,
        timeMs: Long,
        toMove: Boolean,
        gameOver: Boolean,
        isWinner: Boolean,
        baseColorRes: Int,
        dimColorRes: Int,
        normalBg: Drawable,
        activeBg: Drawable,
    ) {
        val timeText = formatClockTime(timeMs)
        if (timeView.text.toString() != timeText) timeView.text = timeText

        val timeColor = when {
            timeMs <= LOW_TIME_MS -> R.color.warning
            toMove && !gameOver -> baseColorRes
            else -> dimColorRes
        }
        val resolved = ContextCompat.getColor(this, timeColor)
        if (timeView.currentTextColor != resolved) timeView.setTextColor(resolved)

        val statusText: Int
        val statusColor: Int
        if (gameOver) {
            statusText = if (isWinner) R.string.status_wins else R.string.status_flagged
            statusColor = if (isWinner) R.color.accent else R.color.warning
        } else if (toMove) {
            statusText = R.string.status_to_move
            statusColor = R.color.accent
        } else {
            statusText = R.string.status_waiting
            statusColor = dimColorRes
        }
        statusView.setText(statusText)
        statusView.setTextColor(ContextCompat.getColor(this, statusColor))

        val target = if (toMove && !gameOver) activeBg else normalBg
        if (panel.background !== target) panel.background = target
    }

    /**
     * Cycles the two layout modes. The phone lies on its side during play,
     * so the texts are *always* rotated to face right (both players read the
     * same way); the only difference is which box is on top:
     *  0 = white on top, black below,
     *  1 = flipped: black on top, white below.
     */
    private fun applyLayoutRotation() {
        val flipped = settings.layoutRotation % 2 == 1
        orderPanels(whiteFirst = !flipped)
        binding.whiteContent.rotation = 90f // tops of the glyphs face right
        binding.blackContent.rotation = 90f
        // The rotated time runs along the box's long axis; scale it to fit
        // that axis on whatever device we are on.
        val sample = formatClockTime(clock?.control?.baseSeconds?.times(1000L) ?: 0L)
        binding.whiteTime.post { fitTimeForRotation(binding.whiteTime, binding.whitePanel, sample) }
        binding.blackTime.post { fitTimeForRotation(binding.blackTime, binding.blackPanel, sample) }
    }

    /** Swaps the panel order (the panels themselves keep their size/weight). */
    private fun orderPanels(whiteFirst: Boolean) {
        val container = binding.panelContainer
        val first = if (whiteFirst) binding.whitePanel else binding.blackPanel
        if (container.getChildAt(0) === first) return
        container.removeView(binding.whitePanel)
        container.removeView(binding.blackPanel)
        container.addView(first)
        container.addView(if (whiteFirst) binding.blackPanel else binding.whitePanel)
    }

    /**
     * Sizes the time text so the (rotated, i.e. vertical) time string takes
     * at most [ROTATED_FIT_FRACTION] of the box's long axis: the baseline
     * size is kept if it fits, otherwise scaled down.
     */
    private fun fitTimeForRotation(view: TextView, box: View, sample: String) {
        view.textSize = rotatedTimeSizePx
        val available = box.height * ROTATED_FIT_FRACTION
        if (available <= 0f) return
        val width = view.paint.measureText(sample)
        if (width > available) view.textSize = rotatedTimeSizePx * available / width
    }

    private fun spToPx(sp: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics,
        )

    private fun formatMinutesForInput(minutes: Double): String =
        if (minutes % 1.0 == 0.0) "${minutes.toLong()}" else minutes.toString()

    private fun parseMinutes(text: CharSequence?): Double? =
        text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()

    private fun parseSeconds(text: CharSequence?): Int? =
        text?.toString()?.trim()?.toIntOrNull()?.takeIf { it >= 0 }

    private fun hideKeyboard() {
        val focus = currentFocus ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(focus.windowToken, 0)
        focus.clearFocus()
    }

    private fun toast(resId: Int) =
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TICK_MS = 50L // 20 fps is plenty for tenths-of-a-second displays
        private const val LOW_TIME_MS = 10_000L // turn the clock red below 10 s
        private const val ROTATED_TIME_SP = 40f // baseline; the fit step scales it to the box
        private const val ROTATED_FIT_FRACTION = 0.55f // rotated time fits within this share of the box's long axis

        private val BASE_MIN_RANGE = 0.5..600.0
        private const val MAX_BONUS_SECONDS = 3600
    }
}
