#!/bin/bash
if [ $# -ne 3 ];then
	echo -e "usage: \"testing.sh (int)forkAfter (int)number_of_testing_iterations (string)map\"\nexample usage: \"testing.sh 9 150 maps/medium.map\" \nmeans forkAfter is 9 and amount of tests is 150 on map medium"
	exit 1
fi
FORKAFTER=$1
AMOUNTOFTESTS=$2
MAPPATH=$3
MAPNAME="${MAPPATH#*/}"

TIMEARR=()
FAILCOUNTER=0

for i in $(seq 1 $AMOUNTOFTESTS);do
	MSG=$(java -Xss512m -cp src/main amazed.Main $MAPPATH parallel-$FORKAFTER -1)
	ALLTEXT=$(echo $MSG | grep "Goal found")
	RESULT=$?
	TIMEARR+=($(echo $MSG | grep -Eo '[0-9]+'))
	if [ "$RESULT" -ne 0 ];then
		let FAILCOUNTER=COUNTER+1
		echo "$i Failure"
		echo $MSG
	else 
		echo "$i Success"
		echo $MSG
	fi
done
echo ""
echo "This many failures out of $AMOUNTOFTESTS runs: $FAILCOUNTER"
echo ""
printf -v TIMEARR_d ',%s' "${TIMEARR[@]}"
TIMEARR_d=${TIMEARR_d:1}
echo $TIMEARR_d | tee results/test_${MAPNAME}_fork${FORKAFTER}_iterations${AMOUNTOFTESTS}StepWaitBonus.csv
