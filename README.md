![Cover Photo](https://challengepost-s3-challengepost.netdna-ssl.com/photos/production/software_photos/000/563/412/datas/gallery.jpg)
![Eight, Three, and Six of Spades](https://challengepost-s3-challengepost.netdna-ssl.com/photos/production/software_photos/000/562/943/datas/gallery.jpg)

*Yes, the photos say x of hearts when they're clearly spades. I was just to lazy to take the training photos in the right order*

## Inspiration
A card game called "Screwy Louie" is the center of many an event in my family. To play, you collect cards until you are able to meet the requirements of a given round (e.g. two "books" of three). When you are able to meet that requirement, you can put down your cards for the first time and continue to put them down whenever possible.

The problem comes in the higher round, where there's a solid chance that you won't ever get to put down a card. In this case, it can take a long time to do the mental math, especially with higher value cards. Instead, this app lets your phone do all of the heavy lifting for you!

## What it does
It recognizes the cards that are put on a table in front of it and automatically calculates the score of the cards based off of whatever rules you like to follow. 

For my family's rules this means 5 points for 3 through 7; 10 points for 8 through King; and 20 Points for 2, Ace, and Joker. (2 and Joker are wild cards).

## How I built it
To build this, I used the OpenCV4Android library, which provides many built-ins for analyzing and manipulating images. To recognize cards, the general approach I took is as follows:
1. Read an image from the camera
2. Find the contours in the image that look like cards
3. Perform a perspective transform on these images so that they all fill a 720 * 480 image.
4. Compare each card to the database of known cards (scanned in earlier) by finding the absolute difference between them and choosing the option with the lowest sum.

## Challenges I ran into
- Installing OpenCV in Android Studio takes a bit of time because the official guide is still written for people using eclipse
- Had a pretty big memory leak that kept on crashing the app. Turns out you don't want to use the `new` keyword when dealing with something that has a native backend (OpenCV's `Mat`s, in this case.)
- Figuring out what the Java interface for everything looks like. There are lots of resources for Python and C++, but not nearly as many for Java (and specifically Android)
- The card recognition algorithm isn't perfect, it'll often recognize something like the two of spades as being the four of spades or vice versa.

## Accomplishments that I'm proud of
- Got it mostly working!
- Did it myself!

## What I learned
Lots of basic computer vision principles and fundamentals. It's definitley piqued my interest, so I'll proabably take the Udacity course or something like it in the near future!

## What's next for CardCounter
I'd like to improce the recognition model by using a more advanced machine learning algorithm. This way, I could use the same image manipulation code to get the perspective-transformed cards and could pass them through the neural net (or whatever works best), which would hopefully have better accuracy than my naïve algorithm.
