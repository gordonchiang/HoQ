#!/usr/bin/env bash

# After running the dump_*.sh scripts, this will iterate through all of the capture output and calculate the results

regex="^([[:digit:]]*\.[[:digit:]]*) ([[:digit:]]*\.[[:digit:]]*)"

printf "Dump,Iter1,Iter2,Iter3,Iter4,Iter5,Iter6,Iter7,Iter8,Iter9,Iter10,AverageTime\n" >> output.csv

for dir in ethernet wifi mobile
do
  echo "$dir"

  for subdir in "$dir"/*
    do
      echo -n "$subdir,"

      times=()

      for pcap in "$subdir"/*
      do
        # echo "$pcap"

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

    # get average of captures
    average=`awk '{printf "%.9f\n", ($1+$2+$3+$4+$5+$6+$7+$8+$9+$10)/10}' <<< "${times[0]} ${times[1]} ${times[2]} ${times[3]} ${times[4]} ${times[5]} ${times[6]} ${times[7]} ${times[8]} ${times[9]}"`

    printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' "${times[0]}" "${times[1]}" "${times[2]}" "${times[3]}" "${times[4]}" "${times[5]}" "${times[6]}" "${times[7]}" "${times[8]}" "${times[9]}" "$average"
    printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' "$subdir" "${times[0]}" "${times[1]}" "${times[2]}" "${times[3]}" "${times[4]}" "${times[5]}" "${times[6]}" "${times[7]}" "${times[8]}" "${times[9]}" "$average" >> output.csv

  done

  echo
done
