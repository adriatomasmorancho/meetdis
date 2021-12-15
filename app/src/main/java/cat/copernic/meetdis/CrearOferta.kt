package cat.copernic.meetdis

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.autofill.AutofillValue
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.appcompat.view.SupportActionModeWrapper
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import cat.copernic.meetdis.databinding.FragmentCrearOfertaBinding
import cat.copernic.meetdis.databinding.FragmentRegistreBinding
import cat.copernic.meetdis.databinding.FragmentRegistreFamiliarBinding
import cat.copernic.meetdis.databinding.FragmentRegistreUsuariBinding
import cat.copernic.meetdis.models.Oferta
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.github.dhaval2404.colorpicker.util.setVisibility
import com.google.android.gms.dynamic.SupportFragmentWrapper
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_crear_oferta.*
import kotlinx.android.synthetic.main.fragment_registre.*
import kotlinx.android.synthetic.main.fragment_registre_familiar.*
import kotlinx.android.synthetic.main.fragment_registre_usuari.*
import kotlinx.android.synthetic.main.fragment_registre_usuari.textCognom
import kotlinx.android.synthetic.main.fragment_registre_usuari.textNom
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CrearOferta.newInstance] factory method to
 * create an instance of this fragment.
 */
class CrearOferta : Fragment(), AdapterView.OnItemSelectedListener {

    private val db = FirebaseFirestore.getInstance()

    private val storageRef = FirebaseStorage.getInstance().getReference()

    lateinit var binding: FragmentCrearOfertaBinding

    private var latestTmpUri: Uri? = null


    val takeImageResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                latestTmpUri?.let { uri ->
                    binding.imageCamara.setImageURI(uri)
                }
            }
        }


    private var spinner: Spinner? = null
    private var opcion: String? = null
    private var identificadorOferta: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate<FragmentCrearOfertaBinding>(
            inflater,
            R.layout.fragment_crear_oferta, container, false
        )
        ArrayAdapter.createFromResource(
            requireContext(), R.array.tipus_Events,

            R.layout.spinner_item
        ).also { adapter ->

            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.spinnerEvents.adapter = adapter

        }

        val args = CrearOfertaArgs.fromBundle(requireArguments())

        binding.imageCamara.setOnClickListener {
            escollirCamaraGaleria()


        }

        binding.mapView.setOnClickListener() {

            cridarMapa()

        }
        var tasca1: Job? = null
        binding.bCrear.setOnClickListener { view: View ->




            if (textTitol.text.isNotEmpty() && descripcio.text.isNotEmpty()) {



                val dni: String = args.dni.uppercase();





                var collUsersRef: CollectionReference = db.collection("ofertes")
                val doc = collUsersRef.document()
                var data: HashMap<String, Any> = hashMapOf(
                    "dni" to args.dni.uppercase(),
                    "titol" to textTitol.text.toString(),
                    "descripcio" to descripcio.text.toString(),
                    "data" to textData.text.toString(),
                    "tipus" to opcion.toString()
                )


                doc.set(data)
                identificadorOferta = doc.id
                Log.i("crearOferta", "doc.id: ${doc.id}")

                pujarImatge(view)

                tasca1 = crearCorrutina(
                    5, //Temps de durada de la corrutina 1
                    binding.bCrear, //Botó per activar la corrutina 1
                    binding.progressBarUn, //Progrés de la corrutina 1
                )
                
                view.findNavController()
                    .navigate(CrearOfertaDirections.actionCrearOfertaFragmentToIniciFragment(dni))


            } else {
                val toast =
                    Toast.makeText(requireContext(), "Algun camp esta buit", Toast.LENGTH_LONG)
                toast.show()
            }
        }


        binding.textData.setOnClickListener { showDatePickerDialog() }


        binding.spinnerEvents.onItemSelectedListener = this
        return binding.root


    }

    private val startForActivityGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.data
            //setImageUri només funciona per rutes locals, no a internet
            binding?.imageCamara?.setImageURI(data)

        }
    }

    private fun obrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.action = Intent.ACTION_PICK
        startForActivityGallery.launch(intent)
    }

    private fun obrirCamera() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri

                takeImageResult.launch(uri)
            }
        }
    }

    fun escollirCamaraGaleria() {
        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setTitle("Selecciona l´ opció amb la que vols obtenir la foto")
        alertDialog.setMessage("Selecciona:")
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, "CAMARA"
        ) { dialog, which -> obrirCamera() }
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, "GALERIA"
        ) { dialog, which -> obrirGaleria() }
        alertDialog.show()

    }

    private fun getTmpFileUri(): Uri? {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", activity?.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return activity?.let {
            FileProvider.getUriForFile(
                it.applicationContext,
                "cat.copernic.meetdis.provider",
                tmpFile
            )
        }
    }

    fun pujarImatge(root: View) {
        // pujar imatge al Cloud Storage de Firebase
        // https://firebase.google.com/docs/storage/android/upload-files?hl=es
        val args = CrearOfertaArgs.fromBundle(requireArguments())
        // Creem una referència amb el path i el nom de la imatge per pujar la imatge
        Log.i("OFERTITA", "$identificadorOferta")
        val pathReference = storageRef.child("ofertes/${this.identificadorOferta}")
        val bitmap =
            (binding.imageCamara.drawable as BitmapDrawable).bitmap // agafem la imatge del imageView
        val baos = ByteArrayOutputStream() // declarem i inicialitzem un outputstream

        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            baos
        ) // convertim el bitmap en outputstream
        val data = baos.toByteArray() //convertim el outputstream en array de bytes.

        val uploadTask = pathReference.putBytes(data)
        uploadTask.addOnFailureListener {
            Toast.makeText(
                activity,
                resources.getText(R.string.Error_pujar_foto),
                Toast.LENGTH_LONG
            ).show()


        }.addOnSuccessListener {
            Toast.makeText(activity, resources.getText(R.string.Exit_pujar_foto), Toast.LENGTH_LONG)
                .show()

        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerFragment { day, month, year -> onDateSelected(day, month, year) }

        activity?.let { datePicker.show(it.supportFragmentManager, "Date Picker") }


    }

    fun cridarMapa() {

        val gmmIntentUri =
            Uri.parse("geo:41.210103794078506,1.6732920326085583")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }


    fun onDateSelected(day: Int, month: Int, year: Int) {
        textData.setText("$day/$month/$year")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        opcion = parent?.getItemAtPosition(position).toString()
        Log.i("Registre", "Opció: $opcion")
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }


    @DelicateCoroutinesApi
    private fun crearCorrutina(durada: Int, inici: Button, progres: ProgressBar) = GlobalScope.launch(

        Dispatchers.Main) {

        progres.setVisibility(true)
        progres.progress = 0 //Situem el ProgressBar a l'inici del procés
        delay(5000)
        withContext(Dispatchers.IO) {
            var comptador = 0
            //Inciem el progrés de la barra de progrés
            while (comptador < durada) {
                //Retardem el progrés de la barra
                if(suspensio((durada * 50).toLong())) {
                    comptador++
                    progres.progress = (comptador * 50) / durada
                }
            }
        }

        progres.progress = 0 //Situem el ProgressBar a l'inici del procés
        progres.setVisibility(false)
    }

    suspend fun suspensio(duracio: Long): Boolean {
        delay(duracio)
        return true
    }

}
