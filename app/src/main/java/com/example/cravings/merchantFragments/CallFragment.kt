package com.example.cravings.merchantFragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.cravings.R
import com.example.cravings.baseActivities.VoipActivity

class CallFragment : Fragment() {
    private lateinit var voip: Button
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_call, container, false)
        voip=view.findViewById(R.id.goToVoipButton)

        voip.setOnClickListener {
            val intent = Intent(requireContext(), VoipActivity::class.java)
            startActivity(intent)
        }
        return view
    }
}