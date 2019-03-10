#!/bin/bash
if [ $# -ne 2 ];then
	echo -e "usage: \"testing.sh (int)forkAfter (int)number_of_testing_iterations\"\nexample usage: \"testing.sh 9 150\" \nmeans forkAfter is 9 and amount of tests is 150"
	exit 1
fi
AMOUNTOFTESTS=$2
COUNTER=0
for i in $(seq 1 $AMOUNTOFTESTS);do
	MSG=$(java -cp src/main amazed.Main maps/medium.map parallel-$1 -1)
	ALLTEXT=$(echo $MSG | grep "Goal found")
	RESULT=$?
	if [ "$RESULT" -ne 0 ];then
		let COUNTER=COUNTER+1
		echo "$i Failure"
		echo $MSG
	else 
		echo "$i Success"
		echo $MSG
	fi
done
echo "This many failures out of $AMOUNTOFTESTS :"
echo $COUNTER

