package tech.ivar.ra

import kotlin.math.pow

class RandomGen(seed:Long) {
    val m:Long=2f.pow(31).toLong()
    val a:Long=1103515245
    val c:Long=12345
    var n:Long=-1
    init {
        //seed=None
        n=seed
    }

    fun next():Double {
        //self.n=(self.a*self.n+self.c)%self.m
        //return self.n/self.m
        n=(a*n+c)%m
        return n.toDouble()/m.toDouble()
    }
}