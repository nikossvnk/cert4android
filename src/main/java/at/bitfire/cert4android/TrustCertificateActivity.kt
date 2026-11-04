/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.cert4android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.bitfire.cert4android.databinding.ActivityTrustCertificateBinding
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.CertificateParsingException
import java.security.cert.X509Certificate
import java.security.spec.MGF1ParameterSpec.SHA1
import java.security.spec.MGF1ParameterSpec.SHA256
import java.text.DateFormat
import java.util.logging.Level
import kotlin.concurrent.thread

class TrustCertificateActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_CERTIFICATE = "certificate"
    }

    private lateinit var model: Model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProvider(this)[Model::class.java]
        model.processIntent(intent)

        val binding = ActivityTrustCertificateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        model.issuedFor.observe(this) { binding.issuedFor.text = it }
        model.issuedBy.observe(this) { binding.issuedBy.text = it }
        model.validity.observe(this) { (from, to) ->
            binding.validity.text = getString(R.string.trust_certificate_validity_period_value, from.orEmpty(), to.orEmpty())
        }
        model.sha1.observe(this) { binding.sha1.text = it }
        model.sha256.observe(this) { binding.sha256.text = it }
        model.verifiedByUser.observe(this) { verified ->
            if (binding.verifiedByUser.isChecked != verified)
                binding.verifiedByUser.isChecked = verified
            binding.accept.isEnabled = verified
        }

        binding.verifiedByUser.setOnCheckedChangeListener { _, isChecked ->
            model.verifiedByUser.value = isChecked
        }
        binding.accept.setOnClickListener {
            sendDecision(true)
            finish()
        }
        binding.reject.setOnClickListener {
            sendDecision(false)
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        model.processIntent(intent)
    }

    private fun sendDecision(trusted: Boolean) {
        val intent = Intent(this, CustomCertService::class.java)
        with(intent) {
            action = CustomCertService.CMD_CERTIFICATION_DECISION
            putExtra(CustomCertService.EXTRA_CERTIFICATE, getIntent().getByteArrayExtra(EXTRA_CERTIFICATE))
            putExtra(CustomCertService.EXTRA_TRUSTED, trusted)
        }
        startService(intent)
    }


    class Model: ViewModel() {

        companion object {
            val certFactory = CertificateFactory.getInstance("X.509")!!
        }

        val issuedFor = MutableLiveData<String>()
        val issuedBy = MutableLiveData<String>()

        val validFrom = MutableLiveData<String>()
        val validTo = MutableLiveData<String>()
        /** pair of [validFrom] and [validTo] */
        val validity = MediatorLiveData<Pair<String?, String?>>().apply {
            addSource(validFrom) { value = it to validTo.value }
            addSource(validTo) { value = validFrom.value to it }
        }

        val sha1 = MutableLiveData<String>()
        val sha256 = MutableLiveData<String>()

        val verifiedByUser = MutableLiveData(false)

        fun processIntent(intent: Intent?) {
            intent?.getByteArrayExtra(EXTRA_CERTIFICATE)?.let { raw ->
                thread {
                    val cert = certFactory.generateCertificate(ByteArrayInputStream(raw)) as? X509Certificate ?: return@thread

                    try {
                        val subject = cert.subjectAlternativeNames?.let { altNames ->
                            val sb = StringBuilder()
                            for (altName in altNames) {
                                val name = altName[1]
                                if (name is String)
                                    sb.append("[").append(altName[0]).append("]").append(name).append(" ")
                            }
                            sb.toString()
                        } ?: /* use CN if alternative names are not available */ cert.subjectX500Principal.name
                        issuedFor.postValue(subject)

                        issuedBy.postValue(cert.issuerX500Principal.name)

                        val formatter = DateFormat.getDateInstance(DateFormat.LONG)
                        validFrom.postValue(formatter.format(cert.notBefore))
                        validTo.postValue(formatter.format(cert.notAfter))

                        sha1.postValue(fingerprint(cert, SHA1.digestAlgorithm))
                        sha256.postValue(fingerprint(cert, SHA256.digestAlgorithm))

                    } catch(e: CertificateParsingException) {
                        Constants.log.log(Level.WARNING, "Couldn't parse certificate", e)
                    }
                }
            }
        }

        private fun fingerprint(cert: X509Certificate, algorithm: String) =
                try {
                    val md = MessageDigest.getInstance(algorithm)
                    "$algorithm: ${hexString(md.digest(cert.encoded))}"
                } catch(e: Exception) {
                    e.message ?: "Couldn't create message digest"
                }

        private fun hexString(data: ByteArray) =
                data.joinToString(":") { "%02x".format(it) }

    }

}