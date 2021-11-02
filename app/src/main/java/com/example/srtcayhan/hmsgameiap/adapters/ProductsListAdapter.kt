package com.example.srtcayhan.hmsgameiap.adapters
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.srtcayhan.hmsgameiap.R
import com.example.srtcayhan.hmsgameiap.adapters.ProductsListAdapter.MyViewHolder
import com.example.srtcayhan.hmsgameiap.callbacks.ProductItemClick
import com.example.srtcayhan.hmsgameiap.models.ProductsListModel
import kotlinx.android.synthetic.main.item_productslist.view.*


class ProductsListAdapter(var names: List<ProductsListModel>,private val productItemClick: ProductItemClick) : RecyclerView.Adapter<MyViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MyViewHolder {
        val itemView = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_productslist, viewGroup, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(myViewHolder: MyViewHolder, i: Int) {
        myViewHolder.bindItems(names[i])
    }

    override fun getItemCount(): Int = names.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class MyViewHolder(itemView: View) : ViewHolder(itemView) {

        fun bindItems(productModel: ProductsListModel){
            productModel.let {
                {

                itemView.item_name.text = productModel.name
                itemView.item_price.text = productModel.price
                itemView.item_image.setImageResource(productModel.image)

            }
            }
            itemView.setOnClickListener {
                productItemClick.onClick(productModel)
            }
        }
    }
}