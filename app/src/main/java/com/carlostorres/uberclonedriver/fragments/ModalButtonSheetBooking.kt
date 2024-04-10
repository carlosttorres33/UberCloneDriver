package com.carlostorres.uberclonedriver.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.carlostorres.uberclonedriver.R
import com.carlostorres.uberclonedriver.activities.MapActivity
import com.carlostorres.uberclonedriver.models.Booking
import com.carlostorres.uberclonedriver.providers.AuthProvider
import com.carlostorres.uberclonedriver.providers.BookingProvider
import com.carlostorres.uberclonedriver.providers.GeoProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ModalButtonSheetBooking : BottomSheetDialogFragment() {

    private lateinit var textViewOrigen : TextView
    private lateinit var textViewDestination : TextView
    private lateinit var textViewTimeAndDistance : TextView
    private lateinit var btnAcept : Button
    private lateinit var btnCancel : Button

    private val bookingProvider = BookingProvider()
    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View? {

        val view = inflater.inflate(R.layout.modal_bottom_sheet_booking, container, false)

        textViewOrigen = view.findViewById(R.id.tvOrigin)
        textViewDestination = view.findViewById(R.id.tvDestination)
        textViewTimeAndDistance = view.findViewById(R.id.tvTimeAndDistance)
        btnAcept = view.findViewById(R.id.btnAcept)
        btnCancel = view.findViewById(R.id.btnCancel)

        val data = arguments?.getString("booking")
        val booking = Booking.fromJson(data!!)

        Log.d("Arguments", "${booking?.toJson()}")

        textViewOrigen.text = booking?.origin
        textViewDestination.text = booking?.destination
        textViewTimeAndDistance.text = "${String.format("%.2f", booking?.time)} min - ${String.format("%.2f", booking?.km)}"

        btnAcept.setOnClickListener {
            acceptBooking(booking?.idClient!!)
        }

        btnCancel.setOnClickListener {
            cancelBooking(booking?.idClient!!)
        }

        return view

    }

    private fun cancelBooking(idClient : String){

        bookingProvider.updateStatus(idClient, "cancel").addOnCompleteListener {

            if (it.isSuccessful){

                Toast.makeText(context, "Viaje Cancelado", Toast.LENGTH_SHORT).show()

            } else {

                Toast.makeText(context, "No se pudo cancelar el viaje", Toast.LENGTH_LONG).show()

            }

        }

    }

    private fun acceptBooking(idClient : String){

        bookingProvider.updateStatus(idClient, "accept").addOnCompleteListener {

            if (it.isSuccessful){

                (activity as? MapActivity)?.easyWayLocation?.endUpdates()
                geoProvider.removeLocation(authProvider.getId())
                Toast.makeText(context, "Viaje Aceptado", Toast.LENGTH_SHORT).show()

            } else {

                Toast.makeText(context, "No se pudo aceptar el viaje", Toast.LENGTH_LONG).show()

            }

        }

    }

    companion object{

        const val TAG = "ModalBottomSheet"

    }

}