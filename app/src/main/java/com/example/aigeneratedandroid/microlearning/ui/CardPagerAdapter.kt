package com.example.aigeneratedandroid.microlearning.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.aigeneratedandroid.R
import com.example.aigeneratedandroid.microlearning.model.IdeaCard

/** One full-screen page per idea card; button taps are forwarded to [listener]. */
class CardPagerAdapter(
    private val listener: Listener
) : RecyclerView.Adapter<CardPagerAdapter.CardHolder>() {

    interface Listener {
        fun onLike(card: IdeaCard)
        fun onNotForMe(card: IdeaCard)
        fun onGoDeeper(card: IdeaCard)
    }

    var cards: List<IdeaCard> = emptyList()
        private set

    fun submit(newCards: List<IdeaCard>) {
        cards = newCards
        notifyDataSetChanged()
    }

    override fun getItemCount() = cards.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder =
        CardHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false))

    override fun onBindViewHolder(holder: CardHolder, position: Int) = holder.bind(cards[position])

    inner class CardHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val topic: TextView = view.findViewById(R.id.topicText)
        private val title: TextView = view.findViewById(R.id.titleText)
        private val ascii: TextView = view.findViewById(R.id.asciiText)
        private val insight: TextView = view.findViewById(R.id.insightText)
        private val source: TextView = view.findViewById(R.id.sourceText)
        private val challenge: TextView = view.findViewById(R.id.challengeText)
        private val like: Button = view.findViewById(R.id.likeButton)
        private val skip: Button = view.findViewById(R.id.skipButton)
        private val deeper: Button = view.findViewById(R.id.deeperButton)

        fun bind(card: IdeaCard) {
            topic.text = card.topic
            title.text = card.title
            ascii.text = card.asciiArt
            insight.text = card.insight
            source.text = itemView.context.getString(R.string.source_line, card.sourceName, card.author)
            challenge.text = card.challenge
            like.setOnClickListener { listener.onLike(card) }
            skip.setOnClickListener { listener.onNotForMe(card) }
            deeper.setOnClickListener { listener.onGoDeeper(card) }
        }
    }
}
