package ru.ifmo.se.musician

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
import com.beust.klaxon.Klaxon
import com.here.android.mpa.cluster.ClusterLayer
import com.here.android.mpa.common.*
import com.here.android.mpa.mapping.*
import com.here.android.mpa.mapping.Map
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import ru.ifmo.se.protofiles.CommunicatorGrpc
import ru.ifmo.se.protofiles.EmptyMessage
import ru.ifmo.se.protofiles.Musician
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URL
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

    private var m_marker_image: Image? = null

    private var latt : Double = 0.0
    private var lngg: Double = 0.0

    private var m_tap_marker: MapScreenMarker? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        checkPermissions()
        findViewById<FloatingActionButton>(R.id.profileButton).setOnClickListener {
            val intent = Intent(this, MainMusician::class.java)
            if (m_tap_marker != null) {
                intent.putExtra("Lat", 52.5219)
                intent.putExtra("Lng", 13.4132)
            } else {
                intent.putExtra("Lat", 0.0)
                intent.putExtra("Lng", 0.0)
            }
            startActivity(intent)
        }
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

    class Datas(val lat: Double, val lng: Double, val busy: Int)

    private fun initMap() {
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapfragment) as SupportMapFragment
        val success = MapSettings.setIsolatedDiskCacheRootPath(applicationContext.getExternalFilesDir(null).absolutePath +
                File.separator + ".here-maps", INTENT_NAME)
        if(success){
            mapFragment.init {
                if (it == OnEngineInitListener.Error.NONE) {

                    m_marker_image = Image()
                    try {
                        m_marker_image?.setImageResource(R.drawable.marker)
                    } catch (e: Exception) {
                        Log.e("ForErr", "FUCK!!!")
                    }
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
                                "A\$AP Pocket" -> R.drawable.asap
                                "Dogg" -> R.drawable.snoop
                                "2Pacman" -> R.drawable.twopac
                                "Dr. Der" -> R.drawable.dredre
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


                            /*val littleList = pw.findViewById<LinearLayout>(R.id.little_list)
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
                            }*/
                            val return_but = pw.findViewById<Button>(R.id.return_button)
                            return_but.setOnClickListener {
                                pw.dismiss()
                                map.setCenter(GeoCoordinate(musician.xCoord, musician.yCoord), Map.Animation.LINEAR)
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

                        override fun  onLongPressEvent(pointF : PointF) : Boolean {
                            Log.i("Fuck", "onLongPressEvent")
                            if (m_tap_marker == null) {
                                Log.i("Fuck", "Null")
                                m_tap_marker = MapScreenMarker(
                                    pointF,
                                    m_marker_image
                                )
                                map.addMapObject(m_tap_marker)

                            } else {
                                Log.i("Fuck", "Not Null")
                                m_tap_marker?.setScreenCoordinate(pointF)
                            }
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
                    map.setCenter(GeoCoordinate(52.5200, 13.4050), Map.Animation.NONE)
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
                        doAsync {
                            val result = URL("http://35.228.95.2:3000/get?day=Monday&time=12&n=70").readText()
                            Log.i("ForResult", result)
                            Log.i("ForResultt", "PARSED")
                            val datas = Klaxon().parseArray<Datas>(result)
//                            val datas = Klaxon().parseArray<Datas>("""
//[{"lat": 52.513285, "lng": 13.3086379, "busy": 23}, {"lat": 52.5308414, "lng": 13.3177503, "busy": 27}, {"lat": 52.5306425, "lng": 13.3186259, "busy": 27}, {"lat": 52.5017107, "lng": 13.3269588, "busy": 42}, {"lat": 52.5080639, "lng": 13.3319012, "busy": 0}, {"lat": 52.50662999999999, "lng": 13.33067, "busy": 25}, {"lat": 52.5201521, "lng": 13.3460225, "busy": 73}, {"lat": 52.5460018, "lng": 13.3479864, "busy": 0}, {"lat": 52.5201521, "lng": 13.3460225, "busy": 73}, {"lat": 52.48389299999999, "lng": 13.350982, "busy": 0}, {"lat": 52.4839188, "lng": 13.350969, "busy": 0}, {"lat": 52.5460018, "lng": 13.3479864, "busy": 0}, {"lat": 52.50204899999999, "lng": 13.356539, "busy": 53}, {"lat": 52.49864199999999, "lng": 13.356607, "busy": 0}, {"lat": 52.4900866, "lng": 13.3597431, "busy": 0}, {"lat": 52.49864199999999, "lng": 13.356607, "busy": 0}, {"lat": 52.5043194, "lng": 13.3581959, "busy": 0}, {"lat": 52.5077126, "lng": 13.3626739, "busy": 49}, {"lat": 52.50911620000001, "lng": 13.3655546, "busy": 0}, {"lat": 52.5090131, "lng": 13.3660136, "busy": 0}]
//                            """.trimIndent())
                            if (datas != null) {
                                this@MainActivity.runOnUiThread {
                                    for (data in datas) {
                                        if (data.busy != 0) {
                                            val circle = MapCircle(400.0, GeoCoordinate(data.lat, data.lng))
                                            when (data.busy) {
                                                in 0..30 -> circle.setFillColor(argb(0xa0, 0xf6, 0xe5, 0x8d))
                                                in 30..60 -> circle.setFillColor(argb(0xd9, 0xff, 0xbe, 0x76))
                                                else -> circle.setFillColor(argb(0xf0, 0xf0, 0x93, 0x2b))
                                            }
                                            Log.i("ForRes", data.busy.toString())
                                            map.addMapObject(circle)
                                        }
                                    }
                                }
                            }
                            Log.i("ForResultt", "out")

                        }
                        while(true) {
                            try {
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
                                        tempMusiciansMarkers.add(
                                            MapMarker(
                                                GeoCoordinate(it.xCoord, it.yCoord),
                                                newimage
                                            )
                                        )
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
                                                adapter =
                                                    MusicianAdapter(copyMusicians, this@MainActivity, windowManager)
                                                layoutManager = LinearLayoutManager(this@MainActivity)
                                            }
                                        }
                                }
                                Thread.sleep(10000)
                            } catch (e: java.lang.Exception) {

                            }
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
