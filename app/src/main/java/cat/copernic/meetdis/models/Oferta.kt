package cat.copernic.meetdis.models

import android.util.Log

data class Oferta(val titol: String, val descripcio: String, val dni: String, val data: String, val tipus: String, val imatge: String ) {
    var titolOferta: String? = null
    var descripcioOferta: String? = null
    var dniOferta: String? = null
    var dataOferta: String? = null
    var tipusOferta: String? = null
    var imatgeOferta: String? = null

    init {
        this.titolOferta = titol
        this.descripcioOferta = descripcio
        this.dniOferta = dni
        this.dataOferta = data
        this.tipusOferta = tipus
        this.imatgeOferta = imatge
        Log.i("proba_id", "$imatgeOferta")
        Log.i("proba_id", "$imatge")
    }

}
