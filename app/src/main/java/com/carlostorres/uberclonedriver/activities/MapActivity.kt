package com.carlostorres.uberclonedriver.activities

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.carlostorres.uberclonedriver.R
import com.carlostorres.uberclonedriver.databinding.ActivityMapBinding
import com.carlostorres.uberclonedriver.databinding.ModalBottomSheetBookingBinding
import com.carlostorres.uberclonedriver.fragments.ModalButtonSheetBooking
import com.carlostorres.uberclonedriver.models.Booking
import com.carlostorres.uberclonedriver.providers.AuthProvider
import com.carlostorres.uberclonedriver.providers.BookingProvider
import com.carlostorres.uberclonedriver.providers.GeoProvider
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ListenerRegistration

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {

    private var bookingListener: ListenerRegistration? = null
    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    private var myLocationLatLng: LatLng? = null

    var easyWayLocation: EasyWayLocation? = null

    private var markerDriver: Marker? = null

    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val bookingProvider = BookingProvider()

    private lateinit var mapActivity: MapActivity

    private val modalBooking = ModalButtonSheetBooking()

    private val timer = object : CountDownTimer(20000, 1000){

        override fun onTick(counter: Long) {
            Log.d("Timer", counter.toString())
        }

        override fun onFinish() {
            Log.d("Timer", "ON FINISH")
            modalBooking.dismiss()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        locationPermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION

            )
        )

        binding.btnConnect.setOnClickListener {
            connectDriver()
        }
        binding.btnDisconnect.setOnClickListener {
            disconnectDriver()
        }

        listenerBooking()

    }

    val locationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                when {
                    permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                        Log.d("Localization", "Permiso Concedido")
                        checkIfDriverIsConnected()
//                       easyWayLocation?.startLocation()
                    }

                    permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                        //easyWayLocation?.startLocation()
                        checkIfDriverIsConnected()
                        Log.d("Localization", "Permiso Concedido con limitacion")
                    }

                    else -> {
                        Log.d("Localization", "Permiso Denegado")
                    }
                }

            }

        }

    private fun disconnectDriver(){

        easyWayLocation?.endUpdates()

        if (myLocationLatLng != null){

            geoProvider.removeLocation(authProvider.getId())
            showButtonConnect()

        }

    }

    private fun showModalBooking(booking : Booking){

        val bundle = Bundle()

        bundle.putString("booking", booking.toJson())
        modalBooking.arguments = bundle

        modalBooking.show(supportFragmentManager, ModalButtonSheetBooking.TAG)

        timer.start()

    }

    private fun listenerBooking(){

        bookingListener = bookingProvider.getBooking().addSnapshotListener { snapshot, error ->

            if (error != null){

                Log.d("Firestore","Error: ${error.message}")
                return@addSnapshotListener

            }

            if (snapshot != null){

                if (snapshot.documents.size > 0){

                    val booking = snapshot.documents[0].toObject(Booking::class.java)

                    if (booking?.status == "create"){

                        showModalBooking(booking!!)
                        Log.d("Firestore", "Data: ${booking?.toJson()}")

                    }

                }

            }


        }

    }

    private fun checkIfDriverIsConnected(){

        geoProvider.getLocation(authProvider.getId()).addOnSuccessListener { document ->

            if (document.exists()){

                if (document.contains("l")){

                    connectDriver()

                } else {

                    showButtonConnect()

                }

            } else {

                showButtonConnect()

            }

        }

    }

    private fun saveLocation(){

        if (myLocationLatLng != null){

            geoProvider.saveLocation(authProvider.getId(), myLocationLatLng!!)

        }

    }

    private fun connectDriver(){

        easyWayLocation?.endUpdates()
        easyWayLocation?.startLocation()
        showButtonDisconnect()

    }

    private fun showButtonConnect(){
        binding.btnDisconnect.visibility = View.GONE
        binding.btnConnect.visibility = View.VISIBLE
    }
    private fun showButtonDisconnect(){
        binding.btnDisconnect.visibility = View.VISIBLE
        binding.btnConnect.visibility = View.GONE
    }

    private fun addMarker() {

        val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.uber_car)

        val markerIcon = getMarkerFromDrawable(drawable!!)

        if (markerDriver != null) {
            markerDriver?.remove() //No repetir icono
        }

        if (myLocationLatLng != null) {

            markerDriver = googleMap?.addMarker(
                MarkerOptions()
                    .position(myLocationLatLng!!)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .icon(markerIcon)
            )

        }


    }

    private fun getMarkerFromDrawable(drawable: Drawable): BitmapDescriptor {

        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            70,
            150,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)

        drawable.setBounds(0, 0, 70, 150)

        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)

    }

    override fun onMapReady(map: GoogleMap) {

        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
//        easyWayLocation?.startLocation()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        googleMap?.isMyLocationEnabled = false

        try {

            val succes = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            )

            if (!succes!!){
                Log.d("MAPAS", "Erro al encontrar el estilo")
            }

        }catch (e: Resources.NotFoundException){
            Log.d("MAPAS", "Erro $e")
        }

    }

    override fun locationOn() {

    }

    //Se Obtiene latitud y longitud de la posicion en tiempo real
    override fun currentLocation(location: Location) {
        myLocationLatLng = LatLng(location.latitude, location.longitude)

        googleMap?.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(myLocationLatLng!!).zoom(17f).build()
            )
        )

        addMarker()

        saveLocation()

    }

    override fun locationCancelled() {

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()
        easyWayLocation?.endUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        easyWayLocation?.endUpdates()
        bookingListener?.remove()
    }

}