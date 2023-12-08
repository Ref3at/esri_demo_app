package com.example.esriapplication

import android.app.Activity
import android.os.Bundle
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.httpcore.authentication.CertificateCredential
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.NetworkAuthenticationType
import com.arcgismaps.httpcore.authentication.PasswordCredential
import com.arcgismaps.httpcore.authentication.ServerTrust
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISTiledLayer
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSymbolStyle
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SelectionProperties
import com.example.esriapplication.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume

class MainActivity : AppCompatActivity() {

    val graphicsOverlay = GraphicsOverlay()

    private lateinit var binding: ActivityMainBinding


    // Create a custom TrustManager that trusts all certificates
    private val trustAllCertificates = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Initialize the SSLContext with the custom TrustManager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCertificates, java.security.SecureRandom())

        // verify all hostnames for the default HttpsURLConnection
        HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }
        // Set the SSLContext as the default for HttpsURLConnection
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)


        ArcGISEnvironment.authenticationManager.networkAuthenticationChallengeHandler =
            NetworkAuthenticationChallengeHandler { authenticationChallenge ->
                Toast.makeText(
                    this@MainActivity,
                    "networkAuthenticationChallengeHandler error",
                    Toast.LENGTH_SHORT
                ).show()

                when (authenticationChallenge.networkAuthenticationType) {
                    is NetworkAuthenticationType.UsernamePassword -> {

                        // Create the Password Credential
                        print("####NetworkAuthenticationType error :: UsernamePassword")
                        val credential = PasswordCredential("username", "password")
                        NetworkAuthenticationChallengeResponse.ContinueWithCredential(credential)
                    }

                    is NetworkAuthenticationType.ServerTrust -> {
                        print("####NetworkAuthenticationType error :: ServerTrust")
                        NetworkAuthenticationChallengeResponse.ContinueWithCredential(ServerTrust)
                    }

                    is NetworkAuthenticationType.ClientCertificate -> {

                        print("####NetworkAuthenticationType error ::.ClientCertificate")
                        val selectedAlias = showCertificatePicker(this@MainActivity)
                        selectedAlias?.let {
                            NetworkAuthenticationChallengeResponse.ContinueWithCredential(
                                CertificateCredential(it)
                            )
                        } ?: NetworkAuthenticationChallengeResponse.ContinueAndFail

                        NetworkAuthenticationChallengeResponse.ContinueWithCredential(ServerTrust)
                    }

                    else -> {
                        print("####NetworkAuthenticationType error :: else branch")
                        NetworkAuthenticationChallengeResponse.ContinueAndFailWithError(Throwable("ArcGISEnvironment.authenticationManager else"))
                    }
                }
            }


        // Create an ArcGISTiledLayer object using the World Imagery map URL.
        val tiledLayer = ArcGISTiledLayer(uri = "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer")
//        val tiledLayer = ArcGISTiledLayer(uri = "https://app.oyoon.gov.ae/dubpolesrimap/intsrv/rest/services/Cache/StreetsNightBlue/MapServer")


        val baseMap = Basemap(baseLayer = tiledLayer)
        val archMap = ArcGISMap(baseMap)
        binding.mapView.map= archMap


        lifecycleScope.launch {
            archMap.loadStatus.collect {
                print("###ArchMap loadStatus:: ${it.toString()}")
                if (it.toString().contains("FailedToLoad")){
                    archMap.retryLoad()
                }
            }
        }


        binding.mapView.setViewpoint(Viewpoint(34.0270, -118.8050, 72000.0))
        lifecycle.addObserver(binding.mapView)

    }

    private suspend fun showCertificatePicker(activityContext: Activity): String? =
        suspendCancellableCoroutine { continuation ->
            val aliasCallback = KeyChainAliasCallback { alias ->
                continuation.resume(alias)
            }
            KeyChain.choosePrivateKeyAlias(
                activityContext, aliasCallback, null, null, null, null
            )
        }

}
