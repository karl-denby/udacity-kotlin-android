package com.karldenby.diceroller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import kotlin.random.Random
import io.realm.SyncUser.logInAsync
import io.realm.SyncUser.Callback
import java.nio.file.Files.setOwner
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.karldenby.diceroller.model.Dice
import io.realm.*
import io.realm.com_karldenby_diceroller_model_DiceRealmProxy.insert
import io.realm.kotlin.where
import java.sql.Date


class MainActivity : AppCompatActivity() {

    // assign after inflation
    private lateinit var diceImage: ImageView
    private lateinit var rollButton: Button
    private lateinit var realm: Realm
    private lateinit var realmConfiguration: RealmConfiguration
    private lateinit var credentials: SyncCredentials

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Realm
        Realm.init(this)
        realm = Realm.getDefaultInstance()
        credentials = SyncCredentials.usernamePassword("test-user", "test-password")
        logInAsync(credentials, Constants.AUTH_URL, object : Callback<SyncUser> {
                override fun onError(error: ObjectServerError) {
                    Log.e("REALM", "User error $error")

                }
                override fun onSuccess(result: SyncUser) {
                    Log.i("REALM","We have a user")
                }
            }
        )
        val url = "${Constants.REALM_URL}/~/dice-roller"
        val data = Dice()
        data.number = 6
        realmConfiguration = SyncUser.current().createConfiguration(url)
            .fullSynchronization()
            .schemaVersion(2)
            .initialData { realm ->
                realm.beginTransaction()
                realm.copyToRealmOrUpdate(data)
                realm.commitTransaction()
                Log.i("REALM", "Inserted default data on $url")
            }
            .build()
        // Upload will continue in the background even if we
        // close the Realm immediately.
        Realm.getInstance(realmConfiguration).close()

        // now layout has been inflated
        diceImage = findViewById(R.id.dice_image)
        rollButton= findViewById(R.id.roll_button)
        rollButton.setOnClickListener {
            rollDice()
        }
    }

    private fun rollDice() {

        val result: Int = Random.nextInt(6) + 1
        val data = Dice()
        data.number = result

        val cloud = 1

        realm = Realm.getInstance(realmConfiguration)
        realm.beginTransaction()
        realm.where(Dice :: class.java).equalTo("cloud", cloud)
        realm.copyToRealmOrUpdate(data)
        realm.commitTransaction()


        val drawableResource = when (result) {
            1 -> R.drawable.dice_1
            2 -> R.drawable.dice_2
            3 -> R.drawable.dice_3
            4 -> R.drawable.dice_4
            5 -> R.drawable.dice_5
            else -> R.drawable.dice_6
        }

        diceImage.setImageResource(drawableResource)

    }
}
