package ru.ifmo.se.client
import android.app.Dialog
import android.content.Context
import android.graphics.Color.argb
import android.graphics.Point
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ru.ifmo.se.protofiles.Musician



class MusicianAdapter(private val list: Array<Musician>, val context: Context, val windowManaer: WindowManager): RecyclerView.Adapter<MusicianAdapter.MusicianHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicianHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_row, parent, false)
        return MusicianHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MusicianHolder, i: Int) {
        val musician = list[i]
        holder.name.text = musician.name
        holder.types.text = musician.generesList.joinToString()
        holder.startTime.text = musician.startTime
        holder.endTime.text = musician.endTime
        val id = when (musician.name) {
            "Face" -> R.drawable.face
            "Dog" -> R.drawable.snoop
            "Ed Sheeran" -> R.drawable.ed_sheeran
            "Naruto" -> R.drawable.naruto
            else -> R.drawable.default_profile_pic
        }
        val changeImRunnable: Runnable = object : Runnable {
            override fun run() {
                holder.singerIcon.setImageResource(id)
                holder.singerIcon.requestLayout()
            }
        }

        holder.singerIcon.post(changeImRunnable)

        holder.view.setOnClickListener({
            val pw = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)
            pw.setContentView(R.layout.autor)
            pw.getWindow().setLayout(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT)
            pw.show()

            val singerIcon = pw.findViewById<ImageView>(R.id.image)
            val id = when (musician.name) {
                "Face" -> R.drawable.face
                "Dog" -> R.drawable.snoop
                "Ed Sheeran" -> R.drawable.ed_sheeran
                "Naruto" -> R.drawable.naruto
                else -> R.drawable.default_profile_pic
            }
            val changeImRunnable: Runnable = object : Runnable {
                override fun run() {
//                    singerIcon.setBackgroundResource(id)
                    singerIcon.setImageResource(id)
                    singerIcon.requestLayout()
                }
            }

            singerIcon.post(changeImRunnable)

            val singerName = pw.findViewById<TextView>(R.id.singer_name)
            singerName.text = musician.name

            val singer_styles = pw.findViewById<TextView>(R.id.singer_styles)
            singer_styles.text = musician.generesList.joinToString()

            val stT = pw.findViewById<TextView>(R.id.startTime)
            stT.text = musician.startTime
            stT.setTextColor(argb(0xff, 0xff, 0xff, 0xff))
            val enT = pw.findViewById<TextView>(R.id.endTime)
            enT.text = musician.endTime
            enT.setTextColor(argb(0xff, 0xff, 0xff, 0xff))


            val littleList = pw.findViewById<LinearLayout>(R.id.little_list)
            for (sing in musician.tracksList) {
                val textView = TextView(pw.context)
                textView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                textView.text = sing
                textView.setTextColor(argb(0xff, 0x00, 0x00, 0x00))
                textView.setPadding(58, 4, 4, 4)
                val typeface = ResourcesCompat.getFont(pw.context, R.font.roboto_regular)
                textView.setTypeface(typeface)
                littleList.addView(textView)
            }

            val return_but = pw.findViewById<Button>(R.id.return_button)
            return_but.setOnClickListener {
                pw.dismiss()
            }
        })
    }

    class MusicianHolder(val view: View): RecyclerView.ViewHolder(view){
        val name = view.findViewById<TextView>(R.id.musicianName)
        val types = view.findViewById<TextView>(R.id.musicianTypes)
        val startTime = view.findViewById<TextView>(R.id.startTime)
        val endTime = view.findViewById<TextView>(R.id.endTime)
        val singerIcon = view.findViewById<ImageView>(R.id.musicianImage)
    }
}

