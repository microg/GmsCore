
# Partial RCS Support: Constellation, Asterism, and Testing Documentation

## Summary

This PR provides partial RCS support groundwork for microG by integrating the existing Constellation and Asterism work and adding structured documentation for build/testing status.

## Included

- Integrated Constellation Service work from PR #3359
- Integrated Asterism Service work from PR #3360
- Added RCS implementation status documentation
- Added RCS testing notes and diagnostic workflow

## Current Status

This PR does not fully solve RCS support yet.

The current branch provides partial implementation groundwork. Full RCS message transmission is not yet confirmed.

## Remaining Work

- Tachyon registration support
- DroidGuard challenge compatibility analysis
- Real-device RCS testing
- Message sending and receiving verification
- Locked bootloader verification

## Relation to Issue

Related to #2994.

This PR should not be treated as a complete fix yet. It is intended to help move RCS implementation forward with a clearer implementation and testing baseline.