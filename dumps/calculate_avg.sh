#!/usr/bin/env bash

# After running the dump_*.sh scripts, this will iterate through all of the capture output and average the results

regex="^([[:digit:]]*\.[[:digit:]]*) ([[:digit:]]*\.[[:digit:]]*)"

printf "Dump,AverageTime\n" >> output.csv

for dir in ethernet wifi mobile
do
    echo "$dir"

    for subdir in "$dir"/*
    do
        echo -n "$subdir,"

        times=()

        for pcap in "$subdir"/*
        do
#             echo "$pcap"

            # get start and end of network flow
            timestamps=$(tshark -r "$pcap" -2 -R "ip.src == 10.0.0.1 || ip.src == 10.0.0.2" -T fields -e _ws.col.Time | sed -e 1b -e '$!d' | tr '\n' ' ')

            # subtract the start and end to get total time of network flow
            if [[ $timestamps =~ $regex ]]
            then
                ts1="${BASH_REMATCH[1]}"
                ts2="${BASH_REMATCH[2]}"
                time=`awk '{printf "%.9f\n", $1-$2}' <<< "$ts2 $ts1"`
                times+=( $time )
            else
                echo "Error!"
                exit 1
            fi
        done

#         for value in "${times[@]}"
#         do
#             echo $value
#         done

        # get average of captures
        average=`awk '{printf "%.9f\n", ($1+$2+$3)/3}' <<< "${times[0]} ${times[1]} ${times[2]}"`
        echo $average

        printf '%s,%s\n' "$subdir" "$average" >> output.csv

    done

    echo
done
