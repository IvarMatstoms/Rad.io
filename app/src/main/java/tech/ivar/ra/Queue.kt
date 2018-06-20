package tech.ivar.ra

class Queue(var randomGen: RandomGen, val items: MutableList<QueueItem>, val startTime: Int) {
    var currentItem:UpcomingItem?=null
    var currentIndex:Int?=null
    val allowRepeat:Boolean=false
    var size:Int
    init {
        items.sortBy { it.id }
        size=items.size
    }

    fun fastForward(inputTime: Int?=null) {

        var time:Int = if (inputTime == null) {
            (System.currentTimeMillis() / 1000L).toInt();
        } else {
            inputTime
        }
        if (currentItem != null) {
            if (currentItem!!.endTime > time) {
                return
            }
        }
        while (true) {
            var item=nextItem()
            if (item.endTime > time) {
                break
            }
        }

    }

    fun nextItem():UpcomingItem{
        /*
                random_num=self.random_gen.next()
        if self.allow_repeat or self.current_index == None or self.size==1:
            new_index=int(random_num*self.size)
        else:
            new_index=int(random_num*(self.size-1))
            if new_index == self.current_index:
                new_index=self.size-1

        if self.current_item == None:
            current_time=self.start_time
        else:
            current_time=self.current_item.end_time+1
        upcoming_item=UpcomingItem(self.items[new_index], current_time)
        self.current_index=new_index
        self.current_item=upcoming_item
        return self.current_item
        * */
        val randomNum=randomGen.next()
        var newIndex=-1
        if (allowRepeat || currentIndex==null || size==1){
            newIndex=(randomNum*size).toInt()
        } else {
            newIndex=(randomNum*(size-1)).toInt()
            if (newIndex == currentIndex) {
                newIndex=size-1
            }
        }
        val currentTime:Int = if (currentItem == null ) {
            startTime
        } else {
            currentItem?.endTime!!+1
        }
        val upcomingItem=UpcomingItem(items.get(newIndex), currentTime)
        currentIndex=newIndex
        currentItem=upcomingItem
        return currentItem!!
    }
}

data class UpcomingItem(val item:QueueItem, val startTime:Int) {
    val endTime:Int
    init {
        endTime=startTime+item.length
    }
}

abstract class QueueItem(val length:Int, val id:String){
    abstract fun getItems():List<Track>

}