package com.karldenby.diceroller.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

open class Dice : RealmObject() {
    @PrimaryKey
    var cloud: Int = 1

    @Required
    var number: Int? = null
}