"""Copyright (c) 2014 by Travis Gockel. All rights reserved.

This program is free software: you can redistribute it and/or modify it under the terms of the Apache License
as published by the Apache Software Foundation, either version 2 of the License, or (at your option) any later
version.

Travis Gockel (travis@gockelhut.com)"""
from . import java

def get_generator(name, args):
    if name == 'java':
        return java.JavaGenerator.create_with_args(args)
    else:
        raise ValueError('Unknown generator type "{}"'.format(name))
