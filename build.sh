#!/bin/sh

# This script is put in Public Domain
# Year: 2012
# Author: Mattias Andrée (maandree@kth.se)


## completion
. run.sh --completion--


## package info
program=mastertimekeeper
dirpackage=se.kth.maandree.${program}
srcpackage=$(sed -e 's/\./\//g' <<<$dirpackage)


## create directory for Java binaries
mkdir -p bin
chmod -R 755 bin


## in with resources to bin/
if [ -d res ]; then
    cp -r res bin
fi


## java compiler and jar creator if default is for Java 7
[[ $(javac -version 2>&1 | cut -d . -f 2) = '7' ]] &&
    function javacSeven()
    {   javac "$@"
    } &&
    function jarSeven()
    {   jar "$@"
    }

## java compiler and jar creator if default is not for Java 7
[[ $(javac -version 2>&1 | cut -d . -f 2) = '7' ]] ||
    function javacSeven()
    {   javac7 "$@"
    } &&
    function jarSeven()
    {   jar7 "$@"
    }


## java executer if default is for Java 7
[[ $(echo `java -version 2>&1 | cut -d . -f 2` | cut -d ' ' -f 1) = '7' ]] &&
    function javaSeven()
    {   java "$@"
    }

## java executer if default is not for Java 7
[[ $(echo `java -version 2>&1 | cut -d . -f 2` | cut -d ' ' -f 1) = '7' ]] ||
    function javaSeven()
    {   java7 "$@"
    }


## warnings
warns="-Xlint:all"

## standard parameters
params="-source 7 -target 7 -s src -d bin"


## libraries
jars=''
if [ -d lib ]; then
    jars=`echo $(find lib | grep '\.jar$') | sed -e 's/lib\//:lib\//g' -e 's/ //g'`
fi


## parse options
paramEcho=0
paramEcj=0
paramAnnot=0
paramDoc=0
for opt in "$@"; do
    if [[ $opt = '-ecj' ]]; then
	paramEcj=1
	if [ -f dev/ecj.jar ]; then
	    function _ecj()
	    {   javaSeven -jar dev/ecj.jar "$@"
	    }
	else
	    function _ecj()
	    {   ecj "$@"
	    }
	fi
	if [ -d /opt/java7/jre/lib ]; then
	    function javacSeven()
	    {   _ecj -bootclasspath `echo $(find /opt/java7/jre/lib | grep '\.jar$') | sed -e 's/\/opt\/java7\/jre\/lib\//:\/opt\/java7\/jre\/lib\//g' -e 's/ //g' | dd skip=1 bs=1 2>/dev/null` "$@"
	    }
	else
	    function javacSeven()
	    {   _ecj "$@"
	    }
	fi
	errs="-err:conditionAssign,noEffectAssign,enumIdentifier,hashCode"
	warns=$errs" -warn:allDeadCode,allDeprecation,allOver-ann,all-static-method,assertIdentifier,boxing,charConcat,compareIdentical,constructorName,deadCode,dep-ann,deprecation,"
	warns+="discouraged,emptyBlock,enumSwitch,fallthrough,fieldHiding,finalBound,finally,forbidden,includeAssertNull,indirectStatic,intfAnnotation,intfNonInherited,intfRedundant,"
	warns+="localHiding,maskedCatchBlock,null,nullDereference,over-ann,paramAssign,pkgDefaultMethod,raw,semicolon,serial,static-method,static-access,staticReceiver,suppress,"
	warns+="syncOverride,syntheticAccess,typeHiding,unchecked,unnecessaryElse,unqualifiedField,unusedAllocation,unusedArgument,unusedImport,unusedLabel,unusedLocal,unusedPrivate,"
	warns+="unusedThrown,uselessTypeCheck,varargsCast,warningToken"
	#unused: enumSwitchPedantic,nls,specialParamHiding,super,switchDefault,unavoidableGenericProblems,nullAnnot,tasks
	#sorry: javadoc,resource,unusedTypeArgs
    elif [[ $opt = '-echo' ]]; then
	paramEcho=1
	function javacSeven()
	{   echo "$@"
	}
    elif [[ $opt = '-annot' ]]; then
	paramAnnot=1
    elif [[ $opt = '-doc' ]]; then
	paramDoc=1
    elif [[ $opt = '-q' ]]; then
	warns=''
    fi
done


if [[ $paramDoc = 1 ]]; then
    ## generate javadoc
    docparams="-sourcepath src -source 7 -encoding utf-8 -version -author -charset utf-8 -linksource -sourcetab 8 -keywords -docencoding utf-8 -d doc/javadoc -private"
    javadoc7 $docparams $(find src | grep '\.java$') $(find bin | grep '\.java$') $(find dev/libsrc | grep '\.java$') ||
    javadoc  $docparams $(find src | grep '\.java$') $(find bin | grep '\.java$') $(find dev/libsrc | grep '\.java$')
    
    ## fixing html heads
    old='<meta http-equiv="Content-Type" content="text\/html" charset="utf-8">'
    new='<meta http-equiv="Content-Type" content="text\/html; charset=utf-8">'
    for file in $(find ./doc/javadoc | grep \\.html\$); do
	mv "$file" "$file~"
	sed -e "s/$old/$new/" < "$file~" > "$file"
	rm "$file~"
    done
else
    ## colouriser
    function colourise()
    {
	if [[ $paramEcho = 1 ]]; then
            cat
	elif [[ $paramEcj = 1 ]]; then
	    if [[ -f "dev/colourpipe.ecj.jar" ]]; then
		sed -e 's/invalid warning token: '\''resource'\''. Ignoring warning and compiling//g' | dd "skip=1" "bs=1" 2>/dev/null | javaSeven -jar dev/colourpipe.ecj.jar
		## this sed | dd is for only an old version of colourpipe incompatible with new ecj
	    else
		cat
	    fi
	elif [[ -f "dev/colourpipe.javac.jar" ]]; then
            javaSeven -jar dev/colourpipe.javac.jar
	else
	    cat
	fi
    }
    
    ## exception generation
    if [ -f 'src/se/kth/maandree/javagen/ExceptionGenerator.java' ]; then
        ## compile exception generator
	( javacSeven $warns -cp . $params 'src/se/kth/maandree/javagen/ExceptionGenerator.java'  2>&1
	) | colourise &&
	
        ## generate exceptions code
	javaSeven -ea -cp bin$jars "se.kth.maandree.javagen.ExceptionGenerator" -o bin -- $(find src | grep '\.exceptions$')  2>&1  &&
	echo -e '\n\n\n'  &&
	
        ## generate exceptions binaries
	( javacSeven $warns -cp bin$jars -source 7 -target 7 $(find bin | grep '\.java$')  2>&1
	) | colourise
    fi &&
    
    ## compile annotations and annotation processorors
    if [ -f 'src/'"$srcpackage"'/requires.java' ]; then
        ( javacSeven $warns -cp .:bin$jars $params src/"$srcpackage"/{ATProcessor,requires}.java  2>&1
        ) | colourise
    fi &&
    
    if [[ $paramAnnot = 0 ]]; then
        ## compile paradis
	( javacSeven $warns -cp .:bin$jars $params $(find src | grep '\.java$')  2>&1
	) | colourise &&
	(
            ## make plugin files
	    mkdir -p ~/.${program}/plugins 2>/dev/null
	    for plugin in $(find bin | grep '/Plugin.class$' | sed -e 's/\/Plugin.class//g' -e 's/bin\///g'); do
		cd bin
                jarSeven -cf ~/.${program}/plugins/`echo $plugin | sed -e 's/\//./g'`.jar $(find $plugin | grep '\.class$')
		cd ..
	    done
	    rm -r bin/${dirpackage}/plugins 2>/dev/null
	)
    else
        ## run annotation processor (and compile paradis)
	javacSeven -processor ${package}.ATProcessor -processorpath bin -implicit:class -cp .:bin$jars $params $(find src | grep '\.java$')
    fi
fi
