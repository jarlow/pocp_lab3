#!/bin/bash
AMOUNTOFTESTS=150
COUNTER=0
for i in $(seq 1 $AMOUNTOFTESTS);do
	MSG=$(java -cp src/main amazed.Main maps/medium.map parallel-$1 -1)
	ALLTEXT=$(echo $MSG | grep "Goal found")
	RESULT=$?
	if [ "$RESULT" -ne 0 ];then
		let COUNTER=COUNTER+1
		echo "Failure"
		echo $MSG
	else 
		echo "Success"
		#echo $MSG
	fi
done
echo "This many failures out of $AMOUNTOFTESTS"
echo $COUNTER

