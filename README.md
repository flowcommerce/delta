[![Build Status](https://travis-ci.org/flowcommerce/delta.svg?branch=master)](https://travis-ci.org/flowcommerce/delta)

delta
=====

Flow's continuous delivery management system.

Delta does continuous delivery using the following SAAS providers:

- Docker Hub
    - Automated builds
    - Image repository
- GitHub
    - Code and versioning repository
- AWS
    - Services used are EC2, EC2 Container Service, Elastic Load Balancing (ELB), Auto-scaling groups
- Database
    - Postgresql hosted on RDS

### Database Setup

See: https://github.com/flowcommerce/delta-postgresql

### Compiling and Running

See: https://github.com/flowcommerce/delta/blob/master/DEVELOPER.md
