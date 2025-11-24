package com.example.cravings.baseActivities.common

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cravings.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var btnConfirm: Button
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_map_picker)

        mapView = findViewById(R.id.mapView)
        btnConfirm = findViewById(R.id.btnConfirmLocation)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        val defaultPoint = GeoPoint(30.0444, 31.2357)
        mapView.controller.setCenter(defaultPoint)

        mapView.setOnTouchListener { _, event ->
            val projection = mapView.projection
            val geo = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
            if (event.action == MotionEvent.ACTION_UP) {
                selectedPoint = geo
                placeMarker(geo)
            }
            false
        }

        btnConfirm.setOnClickListener {
            if (selectedPoint == null) {
                Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val resultIntent = intent
            resultIntent.putExtra("picked_lat", selectedPoint!!.latitude)
            resultIntent.putExtra("picked_lng", selectedPoint!!.longitude)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun placeMarker(point: GeoPoint) {
        marker?.let { mapView.overlays.remove(it) }
        marker = Marker(mapView)
        marker?.position = point
        marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker?.title = "Selected Location"
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}