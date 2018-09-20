import java.lang.IllegalStateException
import kotlin.coroutines.*

class SimpleCoroutine<T>(val block: suspend SimpleCoroutine<T>.() -> Unit) {
    private var nextStep: Continuation<Unit>
    private var done = false
    private var value: T? = null
    private val finally = object : Continuation<Unit>{
        override val context: CoroutineContext
            get() =  EmptyCoroutineContext

        override fun resumeWith(result: SuccessOrFailure<Unit>) {
            print("[X]")
            done = true
        }

    }

    init{
        nextStep = block.createCoroutine(this, finally )
        println("[I]")
    }

    val isDone get() = done

    fun attach(): T {
        if (done)
                throw IllegalStateException("Cannot attach finished SimpleCoroutine")
        print("[a]")
        nextStep.resume(Unit)
        print("[A]")
        return value!!
    }

    suspend fun detach(value: T) {
        print("[d:$value]")
        this.value = value
        suspendCoroutine<Unit> { continuation -> this.nextStep = continuation }
        print("[D]")
    }
}

fun magicNumbers() = SimpleCoroutine {
    print("(1)"); detach(7)
    print("(2)"); detach(9)
    print("(3)"); detach(13)
    print("«Last»")
}

fun main(args: Array<String>) {
    val myCoroutine : SimpleCoroutine<Int> = magicNumbers()

    while (!myCoroutine.isDone) {
        val value = myCoroutine.attach()
        println("<$value>")
    }
    println("End of story")
}