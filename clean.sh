#!/bin/sh

# This script is put in Public Domain
# Year: 2012
# Author: Mattias Andrée (maandree@kth.se)


rm -r bin  2>  /dev/null

rm $(find . | grep '~$')        2>  /dev/null
rm $(find . | grep '\.class$')  2>  /dev/null
rm $(find . | egrep '/(.|)#')   2>  /dev/null

for file in $(find src | grep '\.java$'); do
    expand $file > $file"~"
    mv $file"~" $file
    git add $file
done

