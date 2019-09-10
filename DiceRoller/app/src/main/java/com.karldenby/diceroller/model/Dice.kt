package com.karldenby.diceroller.model

import io.realm.RealmObject
import io.realm.annotations.Required

open class Dice : RealmObject() {
    @Required
    var number: Int? = null
}