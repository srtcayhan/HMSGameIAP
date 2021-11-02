package com.example.srtcayhan.hmsgameiap

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.srtcayhan.hmsgameiap.adapters.ProductsListAdapter
import com.example.srtcayhan.hmsgameiap.callbacks.ProductItemClick
import com.example.srtcayhan.hmsgameiap.databinding.ActivityProductBinding
import com.example.srtcayhan.hmsgameiap.models.ProductsListModel
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hms.iap.Iap
import com.huawei.hms.iap.IapApiException
import com.huawei.hms.iap.IapClient
import com.huawei.hms.iap.entity.*
import kotlinx.android.synthetic.main.activity_product.*
import kotlinx.android.synthetic.main.item_productslist.view.*
import org.json.JSONException


class ProductActivity : AppCompatActivity() {

    companion object {
        private val REQ_CODE_BUY = 4002
        private val TAG = "ProductActivity"
    }

    private val productsListModels = ArrayList<ProductsListModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        itemlist.layoutManager = LinearLayoutManager(this)
        loadProduct()
    }

    /**
     * Load products information and show the products
     */
    private fun loadProduct() {
        // obtain in-app product details configured in AppGallery Connect, and then show the products
        val iapClient = Iap.getIapClient(this)
        val task = iapClient.obtainProductInfo(createProductInfoReq())
        task.addOnSuccessListener { result ->
            if (result != null && !result.productInfoList.isEmpty()) {
                showProduct(result.productInfoList)
            }
        }.addOnFailureListener { e ->
            e.message?.let { Log.e("IAP", it) }
            if (e is IapApiException) {
                val returnCode = e.statusCode
                if (returnCode == OrderStatusCode.ORDER_HWID_NOT_LOGIN) {
                    Toast.makeText(
                        this,
                        "Please sign in to the app with a HUAWEI ID.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createProductInfoReq(): ProductInfoReq? {
        // In-app product type contains:
        // 0: consumable
        // 1: non-consumable
        // 2: auto-renewable subscription
        val req = ProductInfoReq()
        req.let { productDetails ->
            productDetails.priceType = IapClient.PriceType.IN_APP_CONSUMABLE
            val productIds = ArrayList<String>()
            // Pass in the item_productId list of products to be queried.
            // The product ID is the same as that set by a developer when configuring product information in AppGallery Connect.
            productIds.add("12345678")
            Log.i(TAG, "createProductInfoReq: "+productIds)
            productDetails.productIds = productIds
        }
        return req
    }

    private fun showProduct(productInfoList: List<ProductInfo>) {
        for (productInfo in productInfoList) {

            val productsinfo = ProductsListModel(
                productInfo.productName,
                productInfo.price,
                productInfo.productId,
                R.drawable.c_buoycircle_icon
            )
            productsListModels.add(productsinfo)

            val adapter = ProductsListAdapter(productsListModels, productItemClick)
            itemlist.adapter = adapter
        }
    }

    private fun gotoPay(activity: Activity, productId: String?, type: Int) {

        Log.i(TAG, "call createPurchaseIntent")
        val mClient = Iap.getIapClient(activity)
        val task = mClient.createPurchaseIntent(createPurchaseIntentReq(type, productId))
        task.addOnSuccessListener(OnSuccessListener { result ->
            Log.i(TAG, "createPurchaseIntent, onSuccess")
            if (result == null) {
                Log.e(TAG, "result is null")
                return@OnSuccessListener
            }
            val status = result.status
            if (status == null) {
                Log.e(TAG, "status is null")
                return@OnSuccessListener
            }
            // you should pull up the page to complete the payment process.
            if (status.hasResolution()) {
                try {
                    status.startResolutionForResult(activity, REQ_CODE_BUY)
                } catch (exp: IntentSender.SendIntentException) {
                    Log.e(TAG, exp.message.toString())
                }
            } else {
                Log.e(TAG, "intent is null")
            }
        }).addOnFailureListener { e ->
            Log.e(TAG, e.message.toString())
            Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
            if (e is IapApiException) {
                val returnCode = e.statusCode
                Log.e(TAG, "createPurchaseIntent, returnCode: $returnCode")
                // handle error scenarios
            }
        }
    }

    override fun onActivityResult(requestCode: Int,resultCode: Int,data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_BUY) {
            if (data == null) {
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()
                return
            }
            val purchaseResultInfo =Iap.getIapClient(this).parsePurchaseResultInfoFromIntent(data)
            when (purchaseResultInfo.returnCode) {
                OrderStatusCode.ORDER_STATE_SUCCESS -> {
                    // verify signature of payment results.
                    val success: Boolean = CipherUtil.doCheck(purchaseResultInfo.inAppPurchaseData,purchaseResultInfo.inAppDataSignature,resources.getString(R.string.publickey))
                    if (success) {
                        // Call the consumeOwnedPurchase interface to consume it after successfully delivering the product to your user.
                        consumeOwnedPurchase(this, purchaseResultInfo.inAppPurchaseData)
                    } else {
                        Toast.makeText(this, "Pay successful,sign failed", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                OrderStatusCode.ORDER_STATE_CANCEL -> {
                    // The User cancels payment.
                    Toast.makeText(this, "user cancel", Toast.LENGTH_SHORT).show()
                    return
                }
                OrderStatusCode.ORDER_PRODUCT_OWNED -> {
                    // The user has already owned the product.
                    Toast.makeText(this, "you have owned the product", Toast.LENGTH_SHORT).show()
                    // you can check if the user has purchased the product and decide whether to provide goods
                    // if the purchase is a consumable product, consuming the purchase and deliver product
                    return
                }
                else -> Toast.makeText(this, "Pay failed", Toast.LENGTH_SHORT).show()
            }
            return
        }
    }


    private fun createPurchaseIntentReq(type: Int, productId: String?): PurchaseIntentReq {
        val req = PurchaseIntentReq()
        req.let { productDetails ->
            productDetails.productId = productId
            productDetails.priceType = type
            productDetails.developerPayload = "test"
        }
        return req
    }

    var productItemClick: ProductItemClick = object : ProductItemClick {

        override fun onClick(data: ProductsListModel?) {
            val productId: String = data?.id.toString()
            Log.d("productId", "" + productId)
            gotoPay(this@ProductActivity, productId, IapClient.PriceType.IN_APP_CONSUMABLE)
        }
    }

    private fun consumeOwnedPurchase(context: Context, inAppPurchaseData: String) {
        Log.i(TAG,"call consumeOwnedPurchase")
        val mClient = Iap.getIapClient(context)
        val task =mClient.consumeOwnedPurchase(createConsumeOwnedPurchaseReq(inAppPurchaseData))
        task.addOnSuccessListener { // Consume success
            Log.i(TAG,"consumeOwnedPurchase success")
            Toast.makeText(context,"Pay success, and the product has been delivered",Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e(TAG, e.message.toString())
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            if (e is IapApiException) {
                val apiException = e
                val returnCode = apiException.statusCode
                Log.e(TAG,"consumeOwnedPurchase fail,returnCode: $returnCode")
            } else {
                // Other external errors
            }
        }
    }

    private fun createConsumeOwnedPurchaseReq(purchaseData: String): ConsumeOwnedPurchaseReq {
        val req = ConsumeOwnedPurchaseReq()
        // Parse purchaseToken from InAppPurchaseData in JSON format.
        try {
            val inAppPurchaseData = InAppPurchaseData(purchaseData)
            req.purchaseToken = inAppPurchaseData.purchaseToken
        } catch (e: JSONException) {
            Log.e(TAG,"createConsumeOwnedPurchaseReq JSONExeption")
        }
        return req
    }
}
