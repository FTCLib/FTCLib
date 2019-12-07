/*
Copyright (c) 2017 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.onbotjava;

import java.io.File;
import java.io.IOException;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * {@link OnBotJavaDelegatingStandardFileManager} is a delegator to {@link StandardJavaFileManager}.
 * That is, beyond the delegation done in ForwardingJavaFileManager, we here add delegation to the
 * additional StandardJavaFileManager methods.
 */
@SuppressWarnings("WeakerAccess")
public class OnBotJavaDelegatingStandardFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> implements StandardJavaFileManager
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected StandardJavaFileManager delegate;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public OnBotJavaDelegatingStandardFileManager(StandardJavaFileManager delegate)
        {
        super(delegate);
        this.delegate = delegate;
        }

    //---------------------------------------------------------------------------------------------
    // StandardJavaFileManager
    //----------------------------------------------------------------------------------------------

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files)
        {
        return delegate.getJavaFileObjectsFromFiles(files);
        }

    @Override public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files)
        {
        return delegate.getJavaFileObjects(files);
        }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names)
        {
        return delegate.getJavaFileObjectsFromStrings(names);
        }

    @Override public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names)
        {
        return delegate.getJavaFileObjects(names);
        }

    @Override
    public void setLocation(Location location, Iterable<? extends File> path) throws IOException
        {
        delegate.setLocation(location, path);
        }

    @Override public Iterable<? extends File> getLocation(Location location)
        {
        return delegate.getLocation(location);
        }
    }