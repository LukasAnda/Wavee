package sk.lukasanda.sample

import android.animation.ValueAnimator
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val animator = ValueAnimator.ofInt(0,101)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                progressView.progressValue = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        animateButton.setOnClickListener {
            if(animator.isRunning) {
                animator.end()
                return@setOnClickListener
            }
            animator.repeatCount = ValueAnimator.INFINITE
            animator.repeatMode = ValueAnimator.REVERSE
            animator.addUpdateListener {
                progressView.progressValue = it.animatedValue as Int
                progressView.requestLayout()
            }
            animator.duration = 5000
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.start()
        }
    }
}
