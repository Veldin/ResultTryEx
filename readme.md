# Welcome to [result-try-ex](https://github.com/Veldin/ResultTryEx)!

I'm a simple person, with a simple dislike of try-catch. So I build this simple library to simply remove some try-catches from my code.

## What is this?

It's another one of those exception-as-value kinda deals. The 2 classes that are the most important here are

ResultEx -  A little container that either says:
- "I worked! Here's your value" as `Ok<T>`
- "I failed! Here's your exception" as `Error<T>`

ResultTry - A utility class to safely run operations that might throw exception.

## Motivations

Imagine having colleagues who write code… and then you have to use it. 
Jeeze, what nightmare we all face every day.

working with other people’s Java code is like stepping into a pitbull cage fight 
with a blindfold on and your (fem-)d*ck out. You never know if the next method you call is going 
to politely return the wanted value, or explode in your face like a nuclear
Duck flare of exceptions.

Checked exceptions are supposed to keep you safe… 
but half the time your coworkers just half-write them and call the rest “optional.” 
by just throwing some runtime unchecked type shit. Congrats, that’s now your problem.

Tired of code that pretends exceptions are polite guests, when really they’re rabid dogs 
snarling?
ResultEx and ResultTry can wrap those rabid little rat bastards in a 
metaphorical leash so you can carry on with your life without being maimed 
by an unchecked IOException or NullPointerException.

## Motivations (cont)

I've never written rust, but yall call this rust style now right?
Anyway, there are more libs that do the same thing out there, and else Claude
can code you some. The code is not so special, but I wanted to have my own (as of why read Motivations).

note: for you, my code is someone-else's code that handles exceptions shitty.

## The REAL problem (unchecked exceptions)

Most Java APIs confidently tell you:

> “Just catch `IOException` and you’ll be fine.”

That message is a lie.

Web APIs, DB drivers, file systems, JSON parsers, whatever code ,
they throw unchecked exceptions all the damn time.

If you only catch what the method *tells* you about,
everything else escapes and crashes your code **randomly**.

---

## Usage

Let’s start with something real.

### Meet the *very flaky method*

```java
private String veryFlakyMethod() throws IOException { // Trust me, it just throws IoException
    double r = Math.random();

    if (r < 0.33) {
        throw new IOException("Checked IO failure");
    }

    if (r < 0.66) {
        throw new RuntimeException("Unchecked runtime failure"); // Prank
    }

    return "value-" + (int) (r * 1000);
}
```

This method represents real life, imagine you being a consumer of a 
Web request, Database call, File read, or call whatever Library you didn’t write.

The signature tells you to “Just catch IOException.” but in reality you have no idea.

```java
void compareBasics_valueOrFallback() {
    // ======================================================
    // CLASSIC WAY:
    // - Only catches what you are told to catch
    // - Unchecked exceptions kinda just ESCAPE and throw up
    // - THIS CODE FAILS RANDOMLY
    // ======================================================
    String oldValue;
 
    try {
        oldValue = veryFlakyMethod();
    } catch (IOException e) {
        oldValue = "fallback";
    }
 
    // If the RuntimeException happens, we never reach this line
    assertNotNull(oldValue);
}
```

The RuntimeException is just thrown up, and now your code is code that randomly 
fires RuntimeException. Now you are that colleague.

And this stuff is very hard to unit test against, cause during unit tests you control
the environment, you do the Setup, you do the TearDown. In production however, not so much.

```java
void compareBasics_valueOrFallback() {
     // ======================================================
     // NEW WAY:
     // - Checked AND unchecked become values
     // - Nothing escapes
     // - ResultEx is ALWAYS in a known state
     // ======================================================
     ResultEx<String> result = ResultTry.doTry(this::veryFlakyMethod);
     
     String newValue;
    
     if (result.isOk()) {
            newValue = result.unwrap(); // safe because we checked
                                        // (later we go into unwrap() more
     } else {
            newValue = "fallback";
     }
    
     // This line is ALWAYS reached
     assertNotNull(newValue);
    }
```

Now lets try to explicitly get the error message shall we.

```java
void compareBasics_getErrorMessage() {
    // CLASSIC WAY
    String oldMessage;

    try {
        veryFlakyMethod();
        oldMessage = "success";
    } catch (IOException e) {
        oldMessage = e.getMessage();
    }

    // Again, if an RuntimeException occurs we skip this entirely and throw it up.
    assertNotNull(oldMessage);
}
```
In the 𝓝𝓮𝔀 way, I want to introduce yall peasants to fold() (not the phone).
Think of ResultEx like a little container with two possible states:

- Ok - Container contains a successful value (v)
- Error - Container contains a failure (usually an exception)

fold() lets you handle both cases in one place, without ever throwing anything up.

```java
void compareBasics_getErrorMessage() {
    // NEW WAY
    ResultEx<String> result = ResultTry.doTry(this::veryFlakyMethod);
    
    String newMessage = result.fold(
            v -> "success",         // what to do if the result is Ok
            Throwable::getMessage   // what to do if the result is Error (Both checked or unchecked)
    );

    // Always safe
    assertNotNull(newMessage);
}
```
Imagine fold being a functional stand in for

```java
if (result.isOk()) {
    return someValue;
} else {
    return fallbackValue;
}
```

Next to fold() we also have unwrap(), we have already used that in an example above
here somewhere. So if you have been paying attention you might already know what
it does.

Taking the following example (again)

```java
void compareBasics_unwrapVsFold() {
    // ======================================================
    // NEW WAY: ResultEx + unwrap()
    // - Checked AND unchecked both exceptions are wrapped
    // - Unwrap throws if the result is actually an Error
    // - Must check isOk() first to be safe! (or isError is False)
    // ======================================================
    ResultEx<String> result = ResultTry.doTry(this::veryFlakyMethod);

    String newValue;

    if (result.isOk()) {
        // Safe unwrap: we checked isOk()
        newValue = result.unwrap();
    } else {
        // Fallback if the operation failed
        newValue = "fallback";
    }

    // This line is ALWAYS reached
    assertNotNull(newValue);
}
```

unwrap() is basically saying:

> I’m confident this worked, and I want the value now. No questions, no uwu uwu type safety net.

The (faster) unwrap() method is designed to assume that the ResultEx actually contains a 
successful value (Ok). 

If you DO call it on a result that is an Error,
it will throw a filthy nasty yucky UNCHECKED ResultUnwrapException.

Why unchecked? Because we don’t want to force you back into try/catch hell just to get the value. 
And Having to write unwrap() in a try/catch beats the entire g-dmn reason this exist.

Think of it like this: fold() is the responsible adult, unwrap() is asmongold.
<sub><sup>(he doesnt read)

But what if you actually want to handle IOException differently from some RuntimeException?

In plain Java, that usually means nesting catch blocks… catch upon catch upon catch. The problem is, if you don’t really know what the wrapped code might throw, you’re basically wrestling with the language itself.

Let’s assume, though, that we know this particular method can throw a RuntimeException—now what?

```java
void compareBasics_checkErrors() {
    // CLASSIC WAY
    String resultValue;

    try {
        resultValue = veryFlakyMethod();
    } catch (IOException e) { // we knew this could happen
        resultValue = "Caught checked IOException!"; 
    } catch (RuntimeException  e) { // Good think we know this CAN also happen
        resultValue = "Caught unexpected RuntimeException";
    }

    // This line is not guaranteed to run if you miss an exception type
    assertNotNull(resultValue);
}
```

Enter ResultEx and ResultTry:

```java
void compareBasics_checkErrors() {
    // NEW WAY
    ResultEx<String> result = ResultTry.doTry(this::veryFlakyMethod);
    String resultValue;
    
    if (result.isOk()) {
        resultValue = result.unwrap();
    } else {
        // Inspect the type of error without guessing or multiple try/catch
        if (result.isErrorOfType(IOException.class)) {
            resultValue = "Caught checked IOException!";
        } else {
            resultValue = "Caught some other unexpected Exception! " + result.fold(v -> "", Throwable::getMessage);
        }
    }

    assertNotNull(resultValue);
}
```

## TLDR

If you just scrolled to the bottom, welcome. Anyway...

ResultEx and ResultTry are shields against Java’s unpredictable exception throwing jungle.
ResultEx wraps every operation that can fail into a neat little box: 
- Ok<T> if it worked, 
- Error<T> if it blew up. 

No exceptions sneak out, no surprises, no “oh why did it crash now?” moments. 
You can safely handle success or failure with fold(), or grab the value directly with unwrap() 
if you’re feeling bold and not just bald.

ResultTry is the toolbox that can make some of this happen. 
Wrap flaky calls, retries them, chains multiple operations, or even runs them asynchronously (wow!), 
all while treating checked and unchecked exceptions equally.

Basically, it’s like a babysitter for code written by that colleague who can’t type for shit, 
forgets half the exceptions, and thinks try/catch is optional punctuation.