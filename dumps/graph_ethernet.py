#!/usr/bin/env python3

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

dataset = pd.read_csv('output.csv')

main = [None] * 3
main_avgs = [None] * 3
mult = [None] * 3
mult_avgs = [None] * 3
for row in dataset.itertuples():
  dump = row[1]
  if 'ethernet/' in dump:
    list, list_avgs, index = None, None, None
    if 'main' in dump:
      list = main
      list_avgs = main_avgs
    elif 'mult' in dump:
      list = mult
      list_avgs = mult_avgs

    if 'no_tls' in dump: index = 0
    elif 'quic' in dump: index = 2
    elif 'tls' in dump: index = 1

    list[index] = (row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9], row[10], row[11])
    list_avgs[index] = row[12]

print(main)
print(mult)

fig, axs = plt.subplots(2, 1, constrained_layout=True)

bp1 = axs[0].boxplot(main)
axs[0].set_title('Ethernet, Single Request')
axs[0].set_xticklabels(['TCP', 'TCP+TLS', 'QUIC'])
axs[0].set_ylabel('Duration (s)')

bp2 = axs[1].boxplot(mult)
axs[1].set_title('Ethernet, Batch Request')
axs[1].set_xticklabels(['TCP', 'TCP+TLS', 'QUIC'])
axs[1].set_ylabel('Duration (s)')

for i, line in enumerate(bp1['medians']):
    x, y = line.get_xydata()[1]
    text = ' μ={:.3f} '.format(main_avgs[i])
    axs[0].annotate(text, xy=(x, y))

for i, line in enumerate(bp2['medians']):
    x, y = line.get_xydata()[1]
    text = ' μ={:.3f} '.format(mult_avgs[i])
    axs[1].annotate(text, xy=(x, y))

plt.savefig('ethernet.svg', format='svg')
plt.show()
