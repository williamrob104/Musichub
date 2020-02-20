package com.musichub.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.musichub.R
import com.musichub.concurrent.ResponseListener
import com.musichub.playback.AppleMusicTrackMediaHolder
import com.musichub.resource.*
import com.musichub.singleton.Singleton
import com.musichub.util.SpecialCharacters
import com.musichub.util.messageFormat


class SearchCatalogRecyclerViewAdapter(
    private val context: Context,
    private val mainActivityAction: MainActivityAction,
    _entityList: List<AppleMusicEntity>,
    private val query: String?,
    private val showType: Boolean):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val entityList = _entityList.toMutableList()

    override fun getItemCount(): Int {
        return if (query == null) entityList.size
               else entityList.size + 3
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < entityList.size) {
            when (entityList[position]) {
                is AppleMusicArtist -> 0
                is AppleMusicAlbum -> 1
                is AppleMusicSong -> 2
                is AppleMusicMV -> 3
            }
        }
        else 4
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = when(viewType) {
            0 -> R.layout.item_search_catalog_artist
            1 -> R.layout.item_search_catalog_album
            2 -> R.layout.item_search_catalog_track
            3 -> R.layout.item_search_catalog_track
            4 -> R.layout.item_search_catalog_viewall
            else -> throw IllegalArgumentException()
        }
        val v = LayoutInflater.from(context).inflate(layoutId, parent, false)
        return when(viewType) {
            0 -> SearchDatabaseArtistItem(v)
            1 -> SearchDatabaseAlbumItem(v)
            2 -> SearchDatabaseTrackItem(v)
            3 -> SearchDatabaseTrackItem(v)
            4 -> SearchDatabaseViewallItem(v)
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            0 -> (holder as SearchDatabaseArtistItem).setData(entityList[position] as AppleMusicArtist, showType)
            1 -> (holder as SearchDatabaseAlbumItem).setData(entityList[position] as AppleMusicAlbum, showType)
            2 -> (holder as SearchDatabaseTrackItem).setData(entityList[position] as AppleMusicSong, showType)
            3 -> (holder as SearchDatabaseTrackItem).setData(entityList[position] as AppleMusicMV, showType)
            4 -> (holder as SearchDatabaseViewallItem).setData(position - entityList.size)
        }
    }


    private val labelMusicArtist = context.resources.getString(R.string.label_music_artist)
    private val labelMusicAlbum = context.resources.getString(R.string.label_music_album)
    private val labelMusicTrack = context.resources.getString(R.string.label_music_track)

    private val imageSizePixel = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 50f, context.resources.displayMetrics).toInt()


    private inner class SearchDatabaseArtistItem(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.item_search_catalog_artist_tv_title)
        private val labelTextView: TextView = itemView.findViewById(R.id.item_search_catalog_artist_tv_label)

        init {
            itemView.setOnClickListener {
                val artist = entityList[adapterPosition] as AppleMusicArtist
                if (artist.artistViewUrl != null)
                    mainActivityAction.changeFragment(CatalogArtistFragment.newInstance(artist.artistViewUrl))
            }
        }

        fun setData(artist: AppleMusicArtist, showType: Boolean) {
            titleTextView.text = artist.name
            labelTextView.text = if (showType) labelMusicArtist
                                 else artist.primaryGenre ?: ""
        }
    }


    private inner class SearchDatabaseAlbumItem(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val coverartImageView: ImageView = itemView.findViewById(R.id.item_catalog_album_iv_coverart)
        private val titleTextView: TextView = itemView.findViewById(R.id.item_catalog_album_tv_title)
        private val labelTextView: TextView = itemView.findViewById(R.id.item_catalog_album_tv_label)

        init {
            itemView.setOnClickListener {
                val album = entityList[adapterPosition] as AppleMusicAlbum
                mainActivityAction.changeFragment(CatalogAlbumFragment.newInstance(album.albumViewUrl))
            }
        }

        fun setData(album: AppleMusicAlbum, showType: Boolean) {
            coverartImageView.setImageBitmap(null)
            titleTextView.text = album.title
            labelTextView.text =
                if (showType)
                    "$labelMusicAlbum ${SpecialCharacters.smblkcircle} ${album.artistName}"
                else
                    album.artistName
            coverartImageView.setImageBitmap(null)

            val imageUrl = album.coverart?.sourceByShortSideEquals(imageSizePixel)?.url
            if (imageUrl != null) {
                Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                    override fun onResponse(response: Bitmap) {
                        coverartImageView.setImageBitmap(response)
                    }

                    override fun onError(error: Exception) {}
                })
            }
        }
    }


    private inner class SearchDatabaseTrackItem(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val coverartImageView: ImageView = itemView.findViewById(R.id.item_search_catalog_track_iv_coverart)
        private val titleTextView: TextView = itemView.findViewById(R.id.item_search_catalog_track_tv_title)
        private val labelTextView: TextView = itemView.findViewById(R.id.item_search_catalog_track_tv_label)

        init {
            itemView.setOnClickListener {
                if (entityList[adapterPosition] is AppleMusicSong) {
                    val song = entityList[adapterPosition] as AppleMusicSong
                    mainActivityAction.playMedia(AppleMusicTrackMediaHolder(song), null)
                }
            }
        }

        fun setData(song: AppleMusicSong, showType: Boolean) {
            coverartImageView.setImageBitmap(null)
            titleTextView.text = song.title
            labelTextView.text =
                if(showType)
                    "$labelMusicTrack ${SpecialCharacters.smblkcircle} ${song.artistName}"
                else
                    song.artistName
            coverartImageView.setImageBitmap(null)

            val imageUrl = song.coverart?.sourceByShortSideEquals(imageSizePixel)?.url
            if (imageUrl != null) {
                Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                    override fun onResponse(response: Bitmap) {
                        coverartImageView.setImageBitmap(response)
                    }

                    override fun onError(error: Exception) {}
                })
            }
        }

        fun setData(mv: AppleMusicMV, showType: Boolean) {
            coverartImageView.setImageBitmap(null)
            titleTextView.text = mv.title
            labelTextView.text =
                if (showType)
                    "$labelMusicTrack ${SpecialCharacters.smblkcircle} ${mv.artistName}"
                else
                    mv.artistName
            coverartImageView.setImageBitmap(null)

            val imageUrl = mv.coverart?.sourceByShortSideEquals(imageSizePixel)?.url
            if (imageUrl != null) {
                Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                    override fun onResponse(response: Bitmap) {
                        coverartImageView.setImageBitmap(response)
                    }

                    override fun onError(error: Exception) {}
                })
            }
        }
    }

    private inner class SearchDatabaseViewallItem(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.item_search_catalog_viewall_tv_title)
        private var entityType = 0

        init {
            itemView.setOnClickListener {
                mainActivityAction.changeFragment(
                    SearchCatalogViewallFragment.newInstance(
                        entityType,
                        query!!
                    )
                )
            }
        }

        fun setData(entityType: Int) {
            this.entityType = entityType
            val entityTypeString = context.resources.getString(when(entityType) {
                0 -> R.string.label_music_artists
                1 -> R.string.label_music_albums
                2 -> R.string.label_music_tracks
                else -> throw IllegalArgumentException()
            })

            titleTextView.text = context.resources.getString(R.string.label_term_viewall)
                .messageFormat(entityTypeString)
        }
    }
}