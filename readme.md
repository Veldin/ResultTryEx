# Welcome to [result-try-ex](https://github.com/Veldin/result-try-ex)!

I'm a simple person, with a simple dislike of try-catch. So I build this simple library to simply remove some try-catches from my code.

## What is this?

I'ts another one of those exception-as-value kinda deals. The 2 classes that are the most important here are

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

Checked exceptions are supposed to make you safe, 
But half the time people write only half of em down and get lazy. 
The second yall don't want to deal with checked exceptions, yall just make em Unchecked. 
lol kek your problem now.

Tired of code that pretends exceptions are polite guests, when really they’re rabid dogs snarling?
 ResultEx and ResultTry can wrap those rabid little bastards in a 
metaphorical leash so you can carry on with your life without being maimed 
by an IOException or NullPointerException.

## Motivations (cont)

I've never written rust, but yall call this rust style now right?
Anyway, there are more libs that do the same thing out there, and else Claud
can code you some. THe code is not so special, but I wanted to have my own (as of why read Motivations).

The real motivation? I just don’t want to touch anyone else’s code that handles exeptions.
Because I've seen yall swallow exceptions like you swallow...

note: for you, my code is someone-else's code that handles exceptions shitty.

## Usage

todo: Write this

(For now I recommend looking at my Unittests)