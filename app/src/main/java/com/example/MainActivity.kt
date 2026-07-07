package com.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.CampusAppUI
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CampusViewModel
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultListener {
  private val viewModel: CampusViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Preload Razorpay Checkout resources
    try {
      Checkout.preload(applicationContext)
    } catch (e: Exception) {
      Log.e("Razorpay", "Failed to preload Razorpay", e)
    }

    enableEdgeToEdge()
    setContent {
      val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
      MyApplicationTheme(darkTheme = isDarkMode, dynamicColor = false) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          CampusAppUI(viewModel = viewModel)
        }
      }
    }
  }

  fun startRazorpayPayment(amount: Double, purpose: String) {
    val checkout = Checkout()
    // Test key for safe development sandboxing
    checkout.setKeyID("rzp_test_1234567890")
    
    try {
      val options = JSONObject()
      options.put("name", "Campus ERP Portal")
      options.put("description", purpose)
      options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
      options.put("theme.color", "#001D35")
      options.put("currency", "INR")
      
      // Razorpay expects amount in paise (1 INR = 100 Paise)
      val amountInPaise = (amount * 100).toLong()
      options.put("amount", amountInPaise.toString())
      
      val prefill = JSONObject()
      val profile = viewModel.userProfile.value
      prefill.put("email", profile?.email ?: "student.aarav@gmail.com")
      prefill.put("contact", profile?.phone ?: "+919876543210")
      prefill.put("name", profile?.name ?: "Aarav Sharma")
      options.put("prefill", prefill)
      
      val retryObj = JSONObject()
      retryObj.put("enabled", true)
      retryObj.put("max_count", 4)
      options.put("retry", retryObj)

      viewModel.initiateRazorpayPayment(amount, purpose)
      checkout.open(this, options)
    } catch (e: Exception) {
      Log.e("Razorpay", "Error opening Razorpay payment gateway dialog", e)
      Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
    }
  }

  override fun onPaymentSuccess(razorpayPaymentId: String?) {
    Log.d("Razorpay", "Payment Successful: $razorpayPaymentId")
    Toast.makeText(this, "Payment Successful! Transaction Reference ID: $razorpayPaymentId", Toast.LENGTH_LONG).show()
    viewModel.onRazorpayPaymentSuccess(razorpayPaymentId ?: "TXN_RZP_SUCCESS")
  }

  override fun onPaymentError(code: Int, response: String?) {
    Log.e("Razorpay", "Payment Failed/Cancelled. Code: $code, Response: $response")
    Toast.makeText(this, "Payment Cancelled/Failed: $response", Toast.LENGTH_LONG).show()
    viewModel.onRazorpayPaymentError(code, response ?: "Cancelled by User")
  }
}
