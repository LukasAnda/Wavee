package sk.lukasanda.wavee

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class Wavee @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    // Dynamic Properties.
    private var mCanvasSize: Int = 0
    private var mCanvasHeight: Int = 0
    private var mCanvasWidth: Int = 0
    var amplitudeRatio: Float = 0.toFloat()
        private set
//    private var mWaveBgColor: Int = 0
    private var mWaveColor: Int = 0

    // Properties.
    var centerTitle: String? = ""
        get() = "$progressValue%"
    private var mDefaultWaterLevel: Float = 0.toFloat()
    var waterLevelRatio = 1f
        set(waterLevelRatio) {
            if (this.waterLevelRatio != waterLevelRatio) {
                field = waterLevelRatio
                invalidate()
            }
        }
    var waveShiftRatio = DEFAULT_WAVE_SHIFT_RATIO
        set(waveShiftRatio) {
            if (this.waveShiftRatio != waveShiftRatio) {
                field = waveShiftRatio
                invalidate()
            }
        }
    private var mProgressValue = DEFAULT_WAVE_PROGRESS_VALUE

    // Object used to draw.
    // Shader containing repeated waves.
    private var mWaveShader: BitmapShader? = null
    private val bitmapBuffer: Bitmap? = null
    // Shader matrix.
    private var mShaderMatrix: Matrix? = null
    // Paint to draw wave.
    private var mWavePaint: Paint = Paint().apply { isAntiAlias = true }
    // Paint to draw border.
    private var mBorderPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = dp2px(DEFAULT_BORDER_WIDTH).toFloat()
    }
    // Paint to draw background
    private var mViewBackgroundPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // Point to draw title.

    private var mCenterTitlePaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var mCenterTitleStrokePaint: Paint? = null

    private var arcProgress: Float = 0f
        get() = (progressValue * 3.6).toFloat()

    // Animation.
    private var waveShiftAnim: ObjectAnimator = ObjectAnimator.ofFloat(this,
            "waveShiftRatio",
            0f,
            1f).apply {
        repeatCount = ValueAnimator.INFINITE
        duration = 1000
        interpolator = LinearInterpolator()
    }
    private var mAnimatorSet: AnimatorSet = AnimatorSet()

    private var mContext: Context? = null

    var waveColor: Int
        get() = mWaveColor
        set(color) {
            mWaveColor = color
            updateWaveShader()
            invalidate()
        }

    var borderWidth: Float
        get() = mBorderPaint.strokeWidth
        set(width) {
            mBorderPaint!!.strokeWidth = width
            invalidate()
        }

    var borderColor: Int
        get() = mBorderPaint.color
        set(color) {
            mBorderPaint!!.color = color
            updateWaveShader()
            invalidate()
        }

    var progressValue: Int
        get() = mProgressValue
        set(progress) {
            mProgressValue = progress
            waterLevelRatio = progress.toFloat() / 100
        }

    var centerTitleColor: Int
        get() = mCenterTitlePaint.color
        set(centerTitleColor) {
            mCenterTitlePaint!!.color = centerTitleColor
        }

    var centerTitleSize: Float
        get() = mCenterTitlePaint.textSize
        set(centerTitleSize) {
            mCenterTitlePaint!!.textSize = sp2px(centerTitleSize).toFloat()
        }

    private lateinit var rect: RectF

    init {
        mContext = context
        // Init Wave.
        mShaderMatrix = Matrix()
        // Init Animation
        initAnimation()

        // Load the styled attributes and set their properties
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.Wavee, defStyleAttr, 0)

        // Init Wave
        mWaveColor = attributes.getColor(R.styleable.Wavee_wlv_waveColor, DEFAULT_WAVE_COLOR)

        // Init AmplitudeRatio
        val amplitudeRatioAttr = attributes.getFloat(R.styleable.Wavee_wlv_waveAmplitude, DEFAULT_AMPLITUDE_VALUE) / 1000
        amplitudeRatio = if (amplitudeRatioAttr > DEFAULT_AMPLITUDE_RATIO) DEFAULT_AMPLITUDE_RATIO else amplitudeRatioAttr

        // Init Progress
        mProgressValue = attributes.getInteger(R.styleable.Wavee_wlv_progressValue, DEFAULT_WAVE_PROGRESS_VALUE)
        progressValue = mProgressValue


        // Init Border
        mBorderPaint.color = attributes.getColor(R.styleable.Wavee_wlv_progressColor, DEFAULT_WAVE_COLOR)


        mViewBackgroundPaint.color = attributes.getColor(R.styleable.Wavee_wlv_view_background_Color, DEFAULT_VIEW_BACKGROUND)

        // Init Center Title

        mCenterTitlePaint.color = attributes.getColor(R.styleable.Wavee_wlv_titleColor, DEFAULT_TITLE_COLOR)
        mCenterTitlePaint.textSize = attributes.getDimension(R.styleable.Wavee_wlv_titleSize, sp2px(DEFAULT_TITLE_CENTER_SIZE).toFloat())

        centerTitle = attributes.getString(R.styleable.Wavee_wlv_title)

        attributes.recycle()
    }

    public override fun onDraw(canvas: Canvas) {
        mCanvasSize = canvas.width
        if (canvas.height < mCanvasSize) {
            mCanvasSize = canvas.height
        }
        // Draw Wave.
        // Modify paint shader according to mShowWave state.
        if (mWaveShader == null) mWavePaint.shader = null

        mWaveShader?.let {
            // First call after mShowWave, assign it to our paint.
            if (mWavePaint.shader == null) {
                mWavePaint.shader = it
            }

            // Sacle shader according to waveLengthRatio and amplitudeRatio.
            // This decides the size(waveLengthRatio for width, amplitudeRatio for height) of waves.
            mShaderMatrix!!.setScale(1f, amplitudeRatio / DEFAULT_AMPLITUDE_RATIO, 0f, mDefaultWaterLevel)
            // Translate shader according to waveShiftRatio and waterLevelRatio.
            // This decides the start position(waveShiftRatio for x, waterLevelRatio for y) of waves.
            mShaderMatrix!!.postTranslate(waveShiftRatio * width,
                    (DEFAULT_WATER_LEVEL_RATIO - waterLevelRatio) * height)

            // Assign matrix to invalidate the shader.
            it.setLocalMatrix(mShaderMatrix)

            // Get borderWidth.
            val borderWidth = mBorderPaint.strokeWidth


            //Draw circle background
            val radius = width / 2f - 1f - 20f - borderWidth

            canvas.drawCircle(width / 2f, height / 2f, radius, mViewBackgroundPaint)

            //The progress background
            mBorderPaint.alpha = 50
            canvas.drawArc(rect, 0f, 360f, false, mBorderPaint)

            //The progress value
            mBorderPaint.alpha = 255
            canvas.drawArc(rect, -90f, arcProgress, false, mBorderPaint)

            mWavePaint.alpha = 200


            //Draw text
            if (!TextUtils.isEmpty(centerTitle)) {
                val middle = mCenterTitlePaint.measureText(centerTitle)
                // Draw the centered text
                canvas.drawText(centerTitle!!, (width - middle) / 2,
                        height / 2 - (mCenterTitlePaint.descent() + mCenterTitlePaint.ascent()) / 2, mCenterTitlePaint)
            }
            //Draw foreground wave
            canvas.drawCircle(width / 2f, height / 2f, radius, mWavePaint)

        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasSize = w
        if (h < mCanvasSize)
            mCanvasSize = h

        rect = RectF(borderWidth, borderWidth, width - borderWidth, height - borderWidth)

        updateWaveShader()
    }

    private fun updateWaveShader() {
        if (bitmapBuffer == null || haveBoundsChanged()) {
            bitmapBuffer?.recycle()
            val width = measuredWidth
            val height = measuredHeight
            if (width > 0 && height > 0) {
                val defaultAngularFrequency = 2.0f * Math.PI / DEFAULT_WAVE_LENGTH_RATIO.toDouble() / width.toDouble()
                val defaultAmplitude = height * DEFAULT_AMPLITUDE_RATIO
                mDefaultWaterLevel = height * DEFAULT_WATER_LEVEL_RATIO
                val defaultWaveLength = width.toFloat()

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                val wavePaint = Paint()
                wavePaint.strokeWidth = 2f
                wavePaint.isAntiAlias = true

                // Draw default waves into the bitmap.
                // y=Asin(ωx+φ)+h
                val endX = width + 1
                val endY = height + 1

                val waveY = FloatArray(endX)

                wavePaint.color = adjustAlpha(mWaveColor, 0.3f)
                for (beginX in 0 until endX) {
                    val wx = beginX * defaultAngularFrequency
                    val beginY = (mDefaultWaterLevel + defaultAmplitude * Math.sin(wx)).toFloat()
                    canvas.drawLine(beginX.toFloat(), beginY, beginX.toFloat(), endY.toFloat(), wavePaint)
                    waveY[beginX] = beginY
                }

                wavePaint.color = mWaveColor
                val wave2Shift = (defaultWaveLength / 4).toInt()
                for (beginX in 0 until endX) {
                    canvas.drawLine(beginX.toFloat(), waveY[(beginX + wave2Shift) % endX], beginX.toFloat(), endY.toFloat(), wavePaint)
                }

                // Use the bitamp to create the shader.
                mWaveShader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
                this.mWavePaint!!.shader = mWaveShader
            }
        }
    }

    private fun haveBoundsChanged(): Boolean {
        return measuredWidth != bitmapBuffer!!.width || measuredHeight != bitmapBuffer.height
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureWidth(widthMeasureSpec)
        val height = measureHeight(heightMeasureSpec)
        val imageSize = if (width < height) width else height
        setMeasuredDimension(imageSize, imageSize)


    }

    private fun measureWidth(measureSpec: Int): Int {
        val result: Int
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)

        if (specMode == View.MeasureSpec.EXACTLY) {
            // The parent has determined an exact size for the child.
            result = specSize
        } else if (specMode == View.MeasureSpec.AT_MOST) {
            // The child can be as large as it wants up to the specified size.
            result = specSize
        } else {
            // The parent has not imposed any constraint on the child.
            result = mCanvasWidth
        }
        return result
    }

    private fun measureHeight(measureSpecHeight: Int): Int {
        val result: Int
        val specMode = View.MeasureSpec.getMode(measureSpecHeight)
        val specSize = View.MeasureSpec.getSize(measureSpecHeight)

        if (specMode == View.MeasureSpec.EXACTLY) {
            // We were told how big to be.
            result = specSize
        } else if (specMode == View.MeasureSpec.AT_MOST) {
            // The child can be as large as it wants up to the specified size.
            result = specSize
        } else {
            // Measure the text (beware: ascent is a negative number).
            result = mCanvasHeight
        }
        return result + 2
    }

    /**
     * Set vertical size of wave according to amplitudeRatio.
     *
     * @param amplitudeRatio Default to be 0.05. Result of amplitudeRatio + waterLevelRatio should be less than 1.
     */

    fun setAmplitudeRatio(amplitudeRatio: Int) {
        if (this.amplitudeRatio != amplitudeRatio.toFloat() / 1000) {
            this.amplitudeRatio = amplitudeRatio.toFloat() / 1000
            invalidate()
        }
    }


    fun setCenterTitleStrokeWidth(centerTitleStrokeWidth: Float) {
        mCenterTitleStrokePaint!!.strokeWidth = dp2px(centerTitleStrokeWidth).toFloat()
    }

    fun setCenterTitleStrokeColor(centerTitleStrokeColor: Int) {
        mCenterTitleStrokePaint!!.color = centerTitleStrokeColor
    }

    fun startAnimation() {
        if (!isInEditMode) {
            mAnimatorSet.start()
        }
    }

    fun endAnimation() {
        mAnimatorSet.end()
    }

    fun cancelAnimation() {
        mAnimatorSet.cancel()
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun pauseAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAnimatorSet.pause()
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun resumeAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAnimatorSet.resume()
        }
    }

    /**
     * Sets the length of the animation. The default duration is 1000 milliseconds.
     *
     * @param duration The length of the animation, in milliseconds.
     */
    fun setAnimDuration(duration: Long) {
        waveShiftAnim.duration = duration
    }

    private fun initAnimation() {
        mAnimatorSet.play(waveShiftAnim)
    }

    override fun onAttachedToWindow() {
        startAnimation()
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        cancelAnimation()
        super.onDetachedFromWindow()
    }

    /**
     * Transparent the given color by the factor
     * The more the factor closer to zero the more the color gets transparent
     *
     * @param color  The color to transparent
     * @param factor 1.0f to 0.0f
     * @return int - A transplanted color
     */
    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    /**
     * Paint.setTextSize(float textSize) default unit is px.
     *
     * @param spValue The real size of text
     * @return int - A transplanted sp
     */
    private fun sp2px(spValue: Float) = (spValue * getScaledDensity() + 0.5f).toInt()

    private fun dp2px(dp: Float) = (dp * getDensity() + 0.5f).toInt()

    private fun getDensity() = context.resources.displayMetrics.density

    private fun getScaledDensity() = context.resources.displayMetrics.scaledDensity

    companion object {

        private val DEFAULT_AMPLITUDE_RATIO = 0.1f
        private val DEFAULT_AMPLITUDE_VALUE = 50.0f
        private val DEFAULT_WATER_LEVEL_RATIO = 0.5f
        private val DEFAULT_WAVE_LENGTH_RATIO = 1.0f
        private val DEFAULT_WAVE_SHIFT_RATIO = 0.0f
        private val DEFAULT_WAVE_PROGRESS_VALUE = 0
        private val DEFAULT_WAVE_COLOR = Color.parseColor("#358bef")
        private val DEFAULT_VIEW_BACKGROUND = Color.parseColor("#676767")
        private val DEFAULT_TITLE_COLOR = Color.parseColor("#ffffff")
        private val DEFAULT_BORDER_WIDTH = 5f

        private val DEFAULT_TITLE_CENTER_SIZE = 22.0f
    }
}



