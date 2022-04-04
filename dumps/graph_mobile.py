#!/usr/bin/env python3

from copy import deepcopy

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

def genList(d, delay, loss):
  return [ d['TCP'][delay][loss], d['TCP+TLS'][delay][loss], d['QUIC'][delay][loss] ]

def getVal(l):
  return [ l[0]['val'], l[1]['val'], l[2]['val'] ]

def getAvg(l):
  return ( l[0]['avg'], l[1]['avg'], l[2]['avg'] )

def genPlot(axs, x, y, mode, delay, loss):
  d = None
  if mode == 'Single Request': d = main
  elif mode == 'Batch Request': d = mult
  l = genList(d, delay, loss)
  bp = axs[x, y].boxplot(getVal(l))
  title = 'Mobile, {}, {} delay, {} loss'.format(mode, delay, loss)
  axs[x, y].set_title(title)
  axs[x, y].set_xticklabels(['TCP', 'TCP+TLS', 'QUIC'])
  axs[x, y].set_ylabel('Duration (s)')

  for i, line in enumerate(bp['medians']):
      a, b = line.get_xydata()[1]
      text = ' μ={:.3f} '.format(getAvg(l)[i])
      axs[x, y].annotate(text, xy=(a, b))

dataset = pd.read_csv('output.csv')

main = {
  'TCP': {
    '10ms': {
        '1%': { 'val': [None] * 3, 'avg': None },
        '3%': { 'val': [None] * 3, 'avg': None },
        '5%': { 'val': [None] * 3, 'avg': None },
    },
    '25ms': {
        '1%': { 'val': [None] * 3, 'avg': None },
        '3%': { 'val': [None] * 3, 'avg': None },
        '5%': { 'val': [None] * 3, 'avg': None },
    },
    '50ms': {
        '1%': { 'val': [None] * 3, 'avg': None },
        '3%': { 'val': [None] * 3, 'avg': None },
        '5%': { 'val': [None] * 3, 'avg': None },
    },
  },
  'TCP+TLS': {
    '10ms': {
        '1%': { 'val': [None] * 3, 'avg': None },
        '3%': { 'val': [None] * 3, 'avg': None },
        '5%': { 'val': [None] * 3, 'avg': None },
    },
    '25ms': {
        '1%': { 'val': [None] * 3, 'avg': None },
        '3%': { 'val': [None] * 3, 'avg': None },
        '5%': { 'val': [None] * 3, 'avg': None },
    },
    '50ms': {
        '1%': { 'val': [None] * 3, 'avg': None },
        '3%': { 'val': [None] * 3, 'avg': None },
        '5%': { 'val': [None] * 3, 'avg': None },
    },
  },
  'QUIC': {
    '10ms': {
        '1%': { 'val': [None] * 3, 'avg': None },
        '3%': { 'val': [None] * 3, 'avg': None },
        '5%': { 'val': [None] * 3, 'avg': None },
    },
    '25ms': {
        '1%': { 'val': [None] * 3, 'avg': None },
        '3%': { 'val': [None] * 3, 'avg': None },
        '5%': { 'val': [None] * 3, 'avg': None },
    },
    '50ms': {
        '1%': { 'val': [None] * 3, 'avg': None },
        '3%': { 'val': [None] * 3, 'avg': None },
        '5%': { 'val': [None] * 3, 'avg': None },
    },
  },
}

mult = deepcopy(main)

for row in dataset.itertuples():
  dump = row[1]
  if 'mobile/' in dump:
    dict, protocol_index, delay_index, loss_index = None, None, None, None
    if 'main' in dump:
      dict = main
    elif 'mult' in dump:
      dict = mult

    if 'no_tls' in dump: protocol_index = 'TCP'
    elif 'quic' in dump: protocol_index = 'QUIC'
    elif 'tls' in dump: protocol_index = 'TCP+TLS'

    if '50ms' in dump: delay_index = '50ms'
    elif '25ms' in dump: delay_index = '25ms'
    elif '10ms' in dump: delay_index = '10ms'

    if '5%' in dump: loss_index = '5%'
    elif '3%' in dump: loss_index = '3%'
    elif '1%' in dump: loss_index = '1%'

    dict[protocol_index][delay_index][loss_index]['val'] = (row[2], row[3], row[4])
    dict[protocol_index][delay_index][loss_index]['avg'] = row[5]

fig, axs = plt.subplots(3, 3)
fig.subplots_adjust(hspace=0.5, wspace=0.5)

genPlot(axs, 0, 0, 'Single Request', '10ms', '1%')
genPlot(axs, 0, 1, 'Single Request', '10ms', '3%')
genPlot(axs, 0, 2, 'Single Request', '10ms', '5%')
genPlot(axs, 1, 0, 'Single Request', '25ms', '1%')
genPlot(axs, 1, 1, 'Single Request', '25ms', '3%')
genPlot(axs, 1, 2, 'Single Request', '25ms', '5%')
genPlot(axs, 2, 0, 'Single Request', '50ms', '1%')
genPlot(axs, 2, 1, 'Single Request', '50ms', '3%')
genPlot(axs, 2, 2, 'Single Request', '50ms', '5%')

fig2, axs2 = plt.subplots(3, 3)
fig2.subplots_adjust(hspace=0.5, wspace=0.5)

genPlot(axs2, 0, 0, 'Batch Request', '10ms', '1%')
genPlot(axs2, 0, 1, 'Batch Request', '10ms', '3%')
genPlot(axs2, 0, 2, 'Batch Request', '10ms', '5%')
genPlot(axs2, 1, 0, 'Batch Request', '25ms', '1%')
genPlot(axs2, 1, 1, 'Batch Request', '25ms', '3%')
genPlot(axs2, 1, 2, 'Batch Request', '25ms', '5%')
genPlot(axs2, 2, 0, 'Batch Request', '50ms', '1%')
genPlot(axs2, 2, 1, 'Batch Request', '50ms', '3%')
genPlot(axs2, 2, 2, 'Batch Request', '50ms', '5%')

plt.show()
