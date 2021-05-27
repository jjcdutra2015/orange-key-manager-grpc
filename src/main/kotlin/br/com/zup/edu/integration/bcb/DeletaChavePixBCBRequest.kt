package br.com.zup.edu.integration.bcb

import br.com.zup.edu.pix.ContaAssociada

data class DeletaChavePixBCBRequest(
    val key: String,
    val participant: String = ContaAssociada.ITAU_UNIBANCO_ISPB
)
