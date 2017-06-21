/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.egl.templates

import org.lwjgl.egl.*
import org.lwjgl.generator.*

val NV_stream_remote = "NVStreamRemote".nativeClassEGL("NV_stream_remote", postfix = NV) {
    documentation =
        """
        Native bindings to the $registryLink extension.

        This extension provides a generic framework for establishing stream connections when the producer and consumer endpoints are not associated with the
        same {@code EGLStream} object. The cross-process streams allowed by the ${KHR_stream_cross_process_fd.link} extension are one example of such a
        connection. Other potential examples include streams between objects associated with different {@code EGLDisplays} in a single process, streams between
        processes residing in separate virtual machine partitions on a single system, or streams between entirely separate systems connected via the internet.

        The approach used by {@code KHR_stream_cross_process_fd} has several drawbacks:
        ${ul(
            """
            It creates a new pair of stream creation and query functions which are specific to both the type of stream and the method used for communicating
            the stream's identity. Extending this methodology would require new pairs of functions for every new type of stream connection.
            """,
            """
            It allows creation of the connected stream objects without any indication of which side will be the consumer and which will be the producer. It
            even allows, although this was probably not intended, a cross-process stream to be established, but have both producer and consumer exist in the
            same process, with the other process acting as a voyeur that can observe state and perhaps modify attributes, but not operate on frames.
            """,
            """
            The use of file descriptors generated by EGL in one process and accessed by EGL in another to establish communication has potential security flaws,
            and may not be available at all on some real-time operating systems.
            """
        )}

        Rather than implement new connection types using this model, we propose a more generic approach that does not require a proliferation of new interfaces
        and avoids any ambiguities in how the stream will be used. These extensions can coexist with {@code KHR_stream_cross_process_fd}, but allow for
        more control of stream communication by the applications.

        These extensions provide the framework with which arbitrary stream object pairs can be established, but by themselves are insufficient to create such
        pairs. In addition, a separate extension must be used to determine the protocol by which the two objects will communicate. See for example the
        {@link NVStreamSocket NV_stream_socket} extension.

        Several optional types of separate stream objects are defined, with successive levels of remoteness. It should be assumed that a more remote type will
        be less efficient in transfering images, and therefore applications should choose the least remote type available that is compatible with their use
        cases.

        Requires ${KHR_stream.link}.
        """

    IntConstant(
        "Returned by #QueryStreamKHR() and #QueryStreamAttribKHR() when attribute is #STREAM_STATE_KHR.",

        "STREAM_STATE_INITIALIZING_NV"..0x3240
    )

    IntConstant(
        "Accepted as attribute names in #CreateStreamKHR(), #CreateStreamAttribKHR(), #QueryStreamKHR(), and #QueryStreamAttribKHR().",

        "STREAM_TYPE_NV"..0x3241,
        "STREAM_PROTOCOL_NV"..0x3242,
        "STREAM_ENDPOINT_NV"..0x3243
    )

    IntConstant(
        """
        Accepted by #CreateStreamKHR() and #CreateStreamAttribKHR(), and returned by #QueryStreamKHR() and #QueryStreamAttribKHR(), as value when attribute is
        #STREAM_TYPE_NV, #STREAM_PROTOCOL_NV or #STREAM_ENDPOINT_NV.
        """,

        "STREAM_LOCAL_NV"..0x3244
    )

    IntConstant(
        """
        When {@code NV_stream_cross_object} is present, accepted by #CreateStreamKHR() and #CreateStreamAttribKHR() and returned by #QueryStreamKHR() and
        #QueryStreamAttribKHR() as value for attribute #STREAM_TYPE_NV.
        """,

        "STREAM_CROSS_OBJECT_NV"..0x334D
    )

    IntConstant(
        """
        When {@code NV_stream_cross_display} is present, accepted by #CreateStreamKHR() and #CreateStreamAttribKHR() and returned by #QueryStreamKHR() and
        #QueryStreamAttribKHR() as value for attribute #STREAM_TYPE_NV.
        """,

        "STREAM_CROSS_DISPLAY_NV"..0x334E
    )

    IntConstant(
        """
        When {@code NV_stream_cross_process} is present, accepted as value for attribute #STREAM_TYPE_NV by #CreateStreamKHR() and #CreateStreamAttribKHR().
        When either {@code NV_stream_cross_process} or {@code KHR_stream_cross_process_fd} is present, returned by #QueryStreamKHR() and
        #QueryStreamAttribKHR() for attribute  #STREAM_TYPE_NV.
        """,

        "STREAM_CROSS_PROCESS_NV"..0x3245
    )

    IntConstant(
        """
        When {@code NV_stream_cross_partition} is present, accepted by #CreateStreamKHR() and #CreateStreamAttribKHR() and returned by #QueryStreamKHR() and
        #QueryStreamAttribKHR() as value for attribute #STREAM_TYPE_NV.
        """,

        "STREAM_CROSS_PARTITION_NV"..0x323F
    )

    IntConstant(
        """
        When {@code NV_stream_cross_system} is present, accepted by #CreateStreamKHR() and #CreateStreamAttribKHR() and returned by #QueryStreamKHR() and
        #QueryStreamAttribKHR() as value for attribute #STREAM_TYPE_NV.
        """,

        "STREAM_CROSS_SYSTEM_NV"..0x334F
    )

    IntConstant(
        "If {@code KHR_stream_cross_process_fd} is present, returned by #QueryStreamKHR() and #QueryStreamAttribKHR() when attribute is #STREAM_PROTOCOL_NV.",

        "STREAM_PROTOCOL_FD_NV"..0x3246
    )

    IntConstant(
        """
        Accepted by #CreateStreamKHR() and #CreateStreamAttribKHR(), and returned by #QueryStreamKHR() and #QueryStreamAttribKHR() when attribute is
        #STREAM_ENDPOINT_NV.
        """,

        "STREAM_PRODUCER_NV"..0x3247,
        "STREAM_CONSUMER_NV"..0x3248
    )
}