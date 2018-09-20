# Simple kotlin coroutines

This example is a stripped down version of the [buildSequence](https://github.com/Kotlin/kotlin-coroutines/blob/master/examples/sequence/buildSequence.kt) example in the kotlin documentation.

*This example focuses on co-routines which runs on one thread. It does not address the also important usecase of asynchroneous behaviour.*

The full code [is at github](https://github.com/KaspersKotlinRamblings/SimpleCoroutineExample) (where you are likely reading this file.)

The code is an standard intellij kotlin project , and compiles with the `1.3-M2-release-IJ2018.2-1` plugin.

## Collaboration between coroutine and main

There are a few steps in how a coroutine and a main method works. 

1. The coroutine is created. This does not make it do anything, it is just created.
2. It is `attached`. This mean that the code associated with the coroutine starts to execute. In our case, this can be understood as calling the coroutine.
3. The coroutine runs on its own until itself decide to `detach`. At detaching the coroutine will remember where it was in its execution, store it, and return to were `attach` was called.
4. repeat 2&3 until the coroutine has run to its end.
5. attaching a coroutine which has run to its termination is an error.

In the code below, the value passed to `detach` is returned as result of `attach`.

```kotlin
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
```

In main, the call to magicNumbers returns an instance of SimpleCoroutine, which is then stored in myCoroutine.

The while loop `attach` the coroutine. The first time it is attached, it starts from the beginning, and runs until the first 'detach'.

The SimpleCorutine is implemented as:

```kotlin
class SimpleCoroutine<T>(val block: suspend SimpleCoroutine<T>.() -> Unit) {
    private var nextStep: Continuation<Unit>
    private var done = false
    private var value: T? = null
    private val finally = ...

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
```

The low-level workhorse of the implementation is `Continuation`. It is is an object which can be used to remember where to pick up execution. In the above implementation, we use a variable `nextStep` keep the continuation.

#### Running the example
When we run the sample code, we get the following output:

```
[I]
[a](1)[d:7][A]<7>
[a][D](2)[d:9][A]<9>
[a][D](3)[d:13][A]<13>
[a][D]«Last»[X][A]<13>
End of story
```

1. The first `I` comes from the constructor. In the constructor, `nextStep` is set to be a new coroutine build from `block`, which is passed as argument to the contructor.

    The constructed coroutine is not run at this point time, merely created and ready to be attached.

2. The output `[a](1)` stems from the main method calling `attach()` printing `[a]`. The attach method calls the `resume` method on the contination stored in `nextStep`. The main method sees this as a normal method call. The coroutine resumes from the beginning printing `(1)`. The coroutine runs `detach(7)`.

3. The method `detach` is called with the argument `7`. The output `[d:7]` is printed. The value `7` is stored in the coroutine for later use. The lowlevel `suspendCorutine` is called. The lambda of the lowlevel `suspendCorutine` is passed a continuation we stored in nextStep so the next attach will pick up the `print("[D]")` statement when attached next time. Suspending the coroutine returns control back to the `attach` method.
4. The main method picks up inside the `attach` method, right after the `resume` call. It prints `[A]`, and return the value put aside in 3.
5. Next attach prints `[a][D](2)[d:9][A]<9>`. Notice that the coroutine resumes inside the detach method (printing `[D]` after it has been attached `[a]`)

When the coroutine ends, control is transfered to the contination which was given as second parameter to the `createCoroutine` in `init`. The main clue is to have a piece of code that can do exception handling, but in our code it is just used as:

```kotlin
private val finally = object : Continuation<Unit>{
	override val context: CoroutineContext get() =  EmptyCoroutineContext
	override fun resumeWith(result: SuccessOrFailure<Unit>) {
		print("[X]")
		done = true
	}
}
```

When the coroutine ends, the `resumeWith` is called (as we gave the variable `finally` to the `createCoroutine`). It prints the `[X]`, and resume the `attach` right after the `resume` in `attach`.
