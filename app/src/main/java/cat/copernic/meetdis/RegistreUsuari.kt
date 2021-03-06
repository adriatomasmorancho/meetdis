package cat.copernic.meetdis

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import cat.copernic.meetdis.databinding.FragmentRegistreUsuariBinding
import com.github.dhaval2404.colorpicker.util.setVisibility
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_registre_familiar.*
import kotlinx.android.synthetic.main.fragment_registre_monitor.*
import kotlinx.android.synthetic.main.fragment_registre_usuari.*
import kotlinx.android.synthetic.main.fragment_registre_usuari.textCognom
import kotlinx.android.synthetic.main.fragment_registre_usuari.textNom
import java.io.ByteArrayOutputStream
import java.io.File


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RegistreUsuari.newInstance] factory method to
 * create an instance of this fragment.
 */
class RegistreUsuari : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val storageRef = FirebaseStorage.getInstance().getReference()

    lateinit var binding: FragmentRegistreUsuariBinding

    var myCheck: CheckBox? = null
    var finalitza: Button? = null

    private var latestTmpUri: Uri? = null

    val takeImageResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                latestTmpUri?.let { uri ->
                    binding.imageCamara.setImageURI(uri)
                }
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate<cat.copernic.meetdis.databinding.FragmentRegistreUsuariBinding>(
                inflater,
                cat.copernic.meetdis.R.layout.fragment_registre_usuari, container, false
            )


        val args = RegistreUsuariArgs.fromBundle(requireArguments())

        //Camara
        binding.imageCamara.setOnClickListener {
            escollirCamaraGaleria()


        }

        myCheck = binding.checkBoxTerminis
        finalitza = binding.bFinalitzar


        myCheck!!.setOnCheckedChangeListener { buttonView, isChecked ->
            finalitza!!.isEnabled = myCheck!!.isChecked
        }


        binding.bFinalitzar.setOnClickListener { view: View ->


            if (textNom.text.isNotEmpty() && textCognom.text.isNotEmpty()) {
                var dni: String = args.dni;
                dni = dni.lowercase()



                FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                    dni + "@prodis.cat",
                    args.contrasenya
                )

                db.collection("users").document(args.dni).set(
                    hashMapOf(
                        "correu" to dni + "@prodis.cat",
                        "contrasenya" to args.contrasenya,
                        "tipus d??usuari" to args.tipus,
                        "nom" to textNom.text.toString(),
                        "cognoms" to textCognom.text.toString(),
                        "descripcio" to ""
                    )
                )


                db.collection("userspendents").document(args.dni).set(
                    hashMapOf(
                        "correu" to dni + "@prodis.cat",
                        "contrasenya" to args.contrasenya,
                        "tipus d??usuari" to args.tipus,
                        "nom" to textNom.text.toString(),
                        "cognoms" to textCognom.text.toString(),
                        "descripcio" to ""
                    )
                )

                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(
                        dni + "@prodis.cat", //correu electronic
                        args.contrasenya

                    ).addOnCompleteListener() {



                        if (it.isSuccessful) {
                            pujarImatge(view)
                            view.findNavController()
                                .navigate(RegistreUsuariDirections.actionRegistreUsuariFragmentToIniciFragment())
                        }
                    }


            } else {
                val toast =
                    Toast.makeText(requireContext(), "Algun camp esta buit", Toast.LENGTH_LONG)
                toast.show()
            }


        }

        return binding.root

    }

    private val startForActivityGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.data
            //setImageUri nom??s funciona per rutes locals, no a internet
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
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.seleccio_opcio)
        builder.setMessage(R.string.selecciona)
        builder.setPositiveButton(R.string.camara, { dialog, which -> obrirCamera() })
        builder.setNegativeButton(R.string.galeria, { dialog, which -> obrirGaleria() })
        builder.show()
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
        val args = RegistreUsuariArgs.fromBundle(requireArguments())
        // Creem una refer??ncia amb el path i el nom de la imatge per pujar la imatge
        val pathReference = storageRef.child("users/" + args.dni)
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

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
        val navBar: BottomNavigationView =
            activity!!.findViewById(cat.copernic.meetdis.R.id.bottomMenu)
        navBar.setVisibility(visible = false)

    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
        val navBar: BottomNavigationView =
            activity!!.findViewById(cat.copernic.meetdis.R.id.bottomMenu)
        navBar.setVisibility(visible = true)

    }

}

