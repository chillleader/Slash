package ru.ifmo.se.client

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color.argb
import android.graphics.PointF
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.here.android.mpa.cluster.ClusterLayer
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import ru.ifmo.se.protofiles.CommunicatorGrpc
import ru.ifmo.se.protofiles.EmptyMessage
import ru.ifmo.se.protofiles.Musician
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_ASK_PERMISSIONS = 1
    private val REQUIRED_SDK_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val INTENT_NAME = "INIT_MAP"

    private lateinit var map: Map
    private lateinit var mapFragment: SupportMapFragment

    private val musicians = arrayListOf<Musician>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        checkPermissions()
    }

    private fun checkPermissions() {
        val missingPermissions = ArrayList<String>()
        REQUIRED_SDK_PERMISSIONS.forEach {
            val result = ContextCompat.checkSelfPermission(this, it)
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(it)
            }
        }

        if (!missingPermissions.isEmpty()) {
            val permissions = missingPermissions.toTypedArray()
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS)
        }
        initMap()
    }

    private fun initMap() {
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapfragment) as SupportMapFragment
        val success = MapSettings.setIsolatedDiskCacheRootPath(applicationContext.getExternalFilesDir(null).absolutePath +
                File.separator + ".here-maps", INTENT_NAME)
        if(success){
            mapFragment.init {
                if (it == OnEngineInitListener.Error.NONE) {

                    ////
                    mapFragment.mapGesture.addOnGestureListener(object : MapGesture.OnGestureListener {

                        override fun onPanStart() { }

                        override fun onPanEnd() {
                            /* show toast message for onPanEnd gesture callback */
                            Log.i("Fuck", "onPanEnd")
                        }

                        override fun onMultiFingerManipulationStart() { }
                        override fun onMultiFingerManipulationEnd() { }
                        override fun onMapObjectsSelected(list : List<ViewObject>) : Boolean {
                            Log.i( "Fuck" ,"onTapEvent")
                            Log.i( "ForFuck" ,"HERE!!!!!")
                            for (viewObj in list)
                                if (viewObj.getBaseType() == ViewObject.Type.USER_OBJECT) {
                                    if ((viewObj as MapObject).getType() == MapObject.Type.MARKER) {
                                        // At this point we have the originally added
                                        // map marker, so we can do something with it
                                        // (like change the visibility, or more
                                        // marker-specific actions)
                                        val x =(viewObj as MapMarker).coordinate.latitude
                                        val y =(viewObj as MapMarker).coordinate.longitude

                                        var tempMusicians = arrayListOf<Musician>().toTypedArray()
                                        while (tempMusicians.size == 0)
                                            if (!musicians.isEmpty()) {
                                                tempMusicians = musicians.toTypedArray()
                                            }
                                        Log.i("ForX", x.toString())
                                        Log.i("ForY", y.toString())
                                        for (musician in tempMusicians) {
                                            if (musician.xCoord == x && musician.yCoord == y && !musician.name.equals("None")) {
                                                createPopUp(musician)
                                                return false
                                            }
                                        }
                                        return false
                                    }
                                }
                            /*
                             * add map screen marker at coordinates of gesture. if map
                             * screen marker already exists, change to new coordinate
                             */
//                    if (m_tap_marker == null) {
//                        m_tap_marker = new MapScreenMarker(pointF,
//                                m_marker_image);
//                        m_map.addMapObject(m_tap_marker);
//
//                    } else {
//                        m_tap_marker.setScreenCoordinate(pointF);
//                    }

                            // return false to allow the map to handle this callback also
                            return false
                        }

                        private fun createPopUp(musician: Musician) {


                            val pw = Dialog(this@MainActivity)
                            pw.setContentView(R.layout.autor)
//                            pw.setCanceledOnTouchOutside(false)
                            pw.show()

                            val singerIcon = pw.findViewById<ImageView>(R.id.image)
                            val id = when (musician.name) {
                                "Face" -> R.drawable.face
                                "Dog" -> R.drawable.snoop
                                "Ed Sheeran" -> R.drawable.ed_sheeran
                                "Naruto" -> R.drawable.naruto
                                else -> R.drawable.default_profile_pic
                            }
                            singerIcon.setImageResource(id)


                            val singerName = pw.findViewById<TextView>(R.id.singer_name)
                            singerName.text = musician.name

                            val singer_styles = pw.findViewById<TextView>(R.id.singer_styles)
                            singer_styles.text = musician.generesList.joinToString()

                            val stT = pw.findViewById<TextView>(R.id.startTime)
                            stT.text = musician.startTime
                            stT.setTextColor(argb(0xff, 0xff, 0xff, 0xff))
                            stT.textSize = 10f
                            stT.gravity = Gravity.CENTER
                            val enT = pw.findViewById<TextView>(R.id.endTime)
                            enT.textSize = 10f
                            enT.setTextColor(argb(0xff, 0xff, 0xff, 0xff))
                            enT.text = musician.endTime


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



//            <LinearLayout
//            android:layout_width="match_parent"
//            android:layout_height="wrap_content"
//            android:weightSum="1">
//            <TextView
//            android:layout_width="match_parent"
//            android:layout_height="match_parent"
//            android:text="Face - West falling down"
//            android:layout_weight="0.25"
//            android:textColor="#FF000000"
//            android:paddingLeft="48dp" android:fontFamily="@font/roboto_regular_sh"
//
//            />
//            <TextView
//            android:layout_width="match_parent"
//            android:layout_height="match_parent"
//            android:textColor="#FF000000"
//            android:text="playing now"
//
//            android:layout_weight="0.75"
//            android:gravity="right"
//            android:paddingRight="26dp" android:fontFamily="@font/roboto_regular_sh"
//
//            />





                            /////////////////////////////
                        }

                        override fun onTapEvent(pointF : PointF) : Boolean {
                            /* show toast message for onPanEnd gesture callback */
                            return false
                        }

                        override fun  onDoubleTapEvent(pointF : PointF) : Boolean {
                            return false
                        }

                        override fun  onPinchLocked() {

                        }

                        override fun onPinchZoomEvent(v: Float , pointF : PointF) : Boolean {
                            return false
                        }

                        override fun  onRotateLocked() {
                        }

                        override fun  onRotateEvent(v : Float) : Boolean {
                            /* show toast message for onRotateEvent gesture callback */
                            Log.i("Fuck", "onRotateEvent")
                            return false
                        }

                        override fun onTiltEvent(v : Float) : Boolean {
                            return false
                        }

                        override fun  onLongPressEvent(v : PointF) : Boolean {
                            Log.i("Fuck", "onLongPressEvent")
                            return false
                        }

                        override fun onLongPressRelease() : Unit {
                        }

                        override fun  onTwoFingerTapEvent( pointF : PointF) : Boolean {
                            Log.i("Fuck", "onTwoFingerTapEvent")
                            return false
                        }
                    }, 0, false)

                    ////
                    map = mapFragment.map
                    map.setCenter(GeoCoordinate(59.9343, 30.3351), Map.Animation.NONE)
                    map.zoomLevel = (map.maxZoomLevel + map.minZoomLevel) / 2

                    PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS_NETWORK)
                    mapFragment.positionIndicator.isVisible = true

                    val drawable = resources.getDrawable(R.drawable.electric_guitar, theme)
                    val musicianIcon = Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(musicianIcon)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)


                    //////
                    Log.i("ForEach", "None")
                    val unknownDrawable = resources.getDrawable(R.drawable.ic_insta, theme)
                    val unkMusicianIcon = Bitmap.createBitmap(
                        unknownDrawable.intrinsicWidth,
                        unknownDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
                    )
                    Log.i("ForEach", "None")
                    val unkCanvas = Canvas(unkMusicianIcon)
                    Log.i("ForEach", "None")
                    unknownDrawable.setBounds(0, 0, unkCanvas.width, unkCanvas.height)
                    Log.i("ForEach", "None")
                    unknownDrawable.draw(unkCanvas)
                    Log.i("ForEach", "None")
                    /////

                    Log.i("forEach", "Before")
                    var musiciansMarkers = ArrayList<MapMarker>()
                    val tempMusiciansMarkers = ArrayList<MapMarker>()
                    val cl = ClusterLayer()
                    val musiciansForDelete = arrayListOf<Musician>()

                    Thread {
                        while(true) {
                            //map.removeMapObjects(musiciansMarkers.toList())

                            Log.i("forEach", "Executing")

                            GrpcTask(musicians).execute()
                            tempMusiciansMarkers.clear()
                            musiciansForDelete.clear()
                            musicians.forEach {
                                val image = Image()
                                image.bitmap = musicianIcon
                                if (!it.name.equals("None")) {
                                    tempMusiciansMarkers.add(MapMarker(GeoCoordinate(it.xCoord, it.yCoord), image))
                                } else {
                                    musiciansForDelete.add(it)
                                    val newimage = Image()
                                    newimage.bitmap = unkMusicianIcon
                                    tempMusiciansMarkers.add(MapMarker(GeoCoordinate(it.xCoord, it.yCoord), newimage))
                                }
                                Log.i("ForEach", it.name)
                            }


                            if (!tempMusiciansMarkers.isEmpty()) {
                                cl.removeMarkers(musiciansMarkers)

                                musiciansMarkers = tempMusiciansMarkers
                                cl.addMarkers(musiciansMarkers.toList())
                                map.addClusterLayer(cl)
                                val TempMusicians = arrayListOf<Musician>()
                                for (mus in musicians)
                                    if (!mus.name.equals("None"))
                                        TempMusicians.add(mus)

                                val copyMusicians = TempMusicians.toTypedArray()
                                if (copyMusicians.isNotEmpty())
                                    this@MainActivity.runOnUiThread {
                                        findViewById<RecyclerView>(R.id.list).apply {
                                            adapter = MusicianAdapter(copyMusicians, this@MainActivity, windowManager)
                                            layoutManager = LinearLayoutManager(this@MainActivity)
                                        }
                                    }
                            }
                            Thread.sleep(10000)
                        }
                    }.start()

                } else {
                    Log.e("map.init", it.name)
                }
            }
        }
    }
}

private class GrpcTask constructor(_musicians: ArrayList<Musician>) : AsyncTask<Void, Void, String>() {
    private val musicians = _musicians
    private var channel: ManagedChannel? = null

    override fun doInBackground(vararg poof: Void) : String {
//        val host = "10.100.110.201"
        val host = "35.228.95.2"
//        val host = "192.168.43.230"
        val port = 50051
        return try {
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
            val stub = CommunicatorGrpc.newBlockingStub(channel)
            val request = EmptyMessage.newBuilder().build()
            val reply = stub.poll(request)
            Log.i("ForThread", "Before")
            musicians.clear()
            for (musician in reply) {
                musicians.add(musician)
                Log.i("ForThread", musician.name)
            }
            Log.i("ForThread", "OK")
            "OK"
        } catch (e: Exception) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            pw.flush()
            "Failed... : %s".format(sw)
        }
    }

    override fun onPostExecute(poof: String) {
        try {
            channel?.shutdown()?.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}

